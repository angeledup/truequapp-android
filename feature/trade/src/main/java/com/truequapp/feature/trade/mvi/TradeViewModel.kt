package com.truequapp.feature.trade.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.KycLevel
import com.truequapp.core.domain.model.MeetingAppointment
import com.truequapp.core.domain.model.ObfuscatedGeoLocation
import com.truequapp.core.domain.model.SafeZonePoint
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import com.truequapp.core.domain.model.UserProfile
import com.truequapp.core.database.dao.TradeItemDao
import com.truequapp.core.database.dao.TradeProposalDao
import com.truequapp.core.database.mapper.toDomainModel
import com.truequapp.core.database.mapper.toEntity
import com.truequapp.core.domain.rules.BusinessRuleException
import com.truequapp.core.domain.rules.EvaluateCancellationWindowUseCase
import com.truequapp.core.domain.rules.LockInventoryForTradeUseCase
import com.truequapp.core.domain.rules.ValidateTradeValueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel que orquesta el Motor de Propuestas de Trueque de la Fase 2 (CU009, CU010, CU011, CU012).
 * Aplica con rigor las reglas de negocio canónicas y persiste las propuestas en SQLite Room.
 */
@HiltViewModel
class TradeViewModel @Inject constructor(
    private val validateTradeValueUseCase: ValidateTradeValueUseCase = ValidateTradeValueUseCase(),
    private val lockInventoryForTradeUseCase: LockInventoryForTradeUseCase = LockInventoryForTradeUseCase(),
    private val evaluateCancellationWindowUseCase: EvaluateCancellationWindowUseCase = EvaluateCancellationWindowUseCase(),
    private val userSessionDataStore: UserSessionDataStore,
    private val tradeItemDao: TradeItemDao? = null,
    private val tradeProposalDao: TradeProposalDao? = null,
    private val dispatchers: DispatcherProvider
) : BaseViewModel<TradeUiState, TradeUiEvent, TradeUiEffect>(TradeUiState()) {

    // Almacén en memoria de ítems y propuestas para sincronización local
    private val inMemoryItems = mutableListOf<TradeItem>()
    private val inMemoryProposals = mutableListOf<TradeProposal>()

    init {
        seedInitialData()
        loadSessionUser()
    }

    private fun seedInitialData() {
        val seedItems = listOf(
            TradeItem(
                id = "item_001",
                ownerId = "usr_alpha",
                title = "Consola Nintendo Switch OLED",
                description = "En caja original con accesorios completos y dos Joy-Con.",
                categoryId = 2,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 4500.0,
                estimatedValueMax = 5200.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1578303512597-81e6cc155b3e"),
                tagsWishlist = listOf("PlayStation 4", "Tablet Samsung"),
                isAvailable = true
            ),
            TradeItem(
                id = "item_002",
                ownerId = "usr_beta",
                title = "Cálculo de Stewart 8va Edición",
                description = "Libro universitario para ingeniería, pasta dura.",
                categoryId = 3,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.USADO,
                estimatedValueMin = 500.0,
                estimatedValueMax = 700.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c"),
                tagsWishlist = listOf("Álgebra Lineal"),
                isAvailable = true
            ),
            TradeItem(
                id = "item_user_01",
                ownerId = "usr_current",
                title = "Kindle Paperwhite 11va Gen",
                description = "Lector de libros electrónicos con luz cálida regulable.",
                categoryId = 2,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 2000.0,
                estimatedValueMax = 2400.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c"),
                tagsWishlist = emptyList(),
                isAvailable = true
            ),
            TradeItem(
                id = "item_user_02",
                ownerId = "usr_current",
                title = "Audífonos Sony WH-CH520",
                description = "Bluetooth, batería de 50 horas, sonido nítido.",
                categoryId = 2,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.USADO,
                estimatedValueMin = 800.0,
                estimatedValueMax = 1000.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1546435770-a3e426bf472b"),
                tagsWishlist = emptyList(),
                isAvailable = true
            )
        )
        inMemoryItems.addAll(seedItems)

        val seedProposals = listOf(
            TradeProposal(
                id = "prop_seed_101",
                requesterUserId = "usr_beta",
                receiverUserId = "usr_current",
                offeredItemIds = listOf("item_002"),
                targetItemId = "item_user_02",
                compensatoryTruequeCoins = 250.0,
                modality = TradeModality.HIBRIDO_CON_CREDITOS,
                status = TradeStatus.PENDIENTE
            )
        )
        inMemoryProposals.addAll(seedProposals)
    }

    private fun loadSessionUser() {
        viewModelScope.launch(dispatchers.io) {
            userSessionDataStore.sessionFlow.collect { session ->
                val userId = session.userId ?: "usr_current"
                val kyc = if (session.isKycVerified) KycLevel.TELEFONO_SMS_NIVEL_1 else KycLevel.BASICO_NIVEL_0
                val userProfile = UserProfile(
                    id = userId,
                    fullName = session.userName ?: "Usuario Truequapp",
                    username = "truequero_${userId.takeLast(4)}",
                    email = session.userEmail ?: "user_${userId}@truequapp.com",
                    phoneNumber = "+525500000000",
                    kycLevel = kyc,
                    attendanceRate = 1.0,
                    location = ObfuscatedGeoLocation(
                        neighborhoodName = "Centro, Coatzacoalcos",
                        approximateCoordinates = GeoCoordinates(18.1486, -94.4208)
                    )
                )

                setState {
                    copy(
                        currentUserId = userId,
                        truequeCoins = session.truequeCoins,
                        currentUserProfile = userProfile
                    )
                }
                syncDataFromDatabase(userId)
            }
        }
    }

    private suspend fun syncDataFromDatabase(userId: String) {
        // Cargar ítems de SQLite Room para que los artículos publicados por el usuario aparezcan disponibles
        val dbItems = tradeItemDao?.getAllItemsSync().orEmpty()
        if (dbItems.isNotEmpty()) {
            inMemoryItems.clear()
            inMemoryItems.addAll(dbItems.map { it.toDomainModel() })
        }

        // Cargar propuestas desde SQLite Room
        val dbProposals = tradeProposalDao?.getAllProposalsSync().orEmpty()
        if (dbProposals.isNotEmpty()) {
            inMemoryProposals.clear()
            inMemoryProposals.addAll(dbProposals.map { it.toDomainModel() })
        }

        loadProposals(userId)
    }

    override fun onEvent(event: TradeUiEvent) {
        when (event) {
            is TradeUiEvent.InitTradeCreation -> initTradeCreation(event.targetItemId, event.userId)
            is TradeUiEvent.ToggleOfferedItemSelection -> toggleOfferedItem(event.itemId)
            is TradeUiEvent.OnTruequeCoinsChanged -> updateTruequeCoins(event.truequeCoinsStr)
            is TradeUiEvent.OnCompensatoryCoinsChanged -> updateTruequeCoins(event.coinsStr)
            is TradeUiEvent.SubmitProposal -> submitProposal()
            is TradeUiEvent.LoadProposals -> loadProposals(event.userId)
            is TradeUiEvent.SelectTab -> setState { copy(selectedTab = event.tabIndex) }
            is TradeUiEvent.LoadProposalDetails -> loadProposalDetails(event.proposalId, event.currentUserId)
            is TradeUiEvent.AcceptProposal -> acceptProposal(event.proposalId)
            is TradeUiEvent.OpenCounterOfferDialog -> setState { copy(showCounterOfferDialog = true) }
            is TradeUiEvent.DismissCounterOfferDialog -> setState { copy(showCounterOfferDialog = false) }
            is TradeUiEvent.ToggleCounterOfferItemSelection -> toggleCounterOfferItem(event.itemId)
            is TradeUiEvent.OnCounterOfferTruequeCoinsChanged -> updateCounterOfferCoins(event.truequeCoinsStr)
            is TradeUiEvent.OnCounterOfferCoinsChanged -> updateCounterOfferCoins(event.coinsStr)
            is TradeUiEvent.SubmitCounterOffer -> submitCounterOffer()
            is TradeUiEvent.RejectProposal -> rejectProposal(event.proposalId)
            is TradeUiEvent.RequestCancelProposal -> requestCancelProposal(event.proposalId)
            is TradeUiEvent.DismissCancelDialog -> setState { copy(showCancelDialog = false, cancellationWarningMessage = null) }
            is TradeUiEvent.ConfirmCancelProposal -> confirmCancelProposal()
            is TradeUiEvent.DismissError -> setState { copy(errorMessage = null) }
        }
    }

    // CU009: Proponer Trueque Directo o Híbrido
    private fun initTradeCreation(targetItemId: String, userId: String) {
        val effectiveUserId = when {
            userId.isNotBlank() -> userId
            currentState.currentUserId.isNotBlank() -> currentState.currentUserId
            else -> "usr_current"
        }

        // 1. Buscar objetivo en memoria
        var target = inMemoryItems.find { it.id == targetItemId }

        // 2. Anti-auto-trueque: 2 partes obligatorias (no puedes intercambiar contigo mismo)
        if (target != null && target.ownerId == effectiveUserId) {
            setState {
                copy(
                    targetItem = target,
                    currentUserId = effectiveUserId,
                    availableUserItems = emptyList(),
                    selectedOfferedItemIds = emptySet(),
                    canSubmitProposal = false,
                    errorMessage = "Esta publicación es tuya. No puedes realizar un trueque contigo mismo."
                )
            }
            return
        }

        // 3. Artículos disponibles del usuario
        var userItems = inMemoryItems.filter {
            it.ownerId == effectiveUserId && it.status == ItemStatus.DISPONIBLE && it.id != targetItemId
        }

        // Si el usuario no tiene ítems propios registrados aún, asociar ítems de inventario base al usuario activo
        if (userItems.isEmpty() && (effectiveUserId == "usr_current" || effectiveUserId.startsWith("usr_demo"))) {
            inMemoryItems.forEachIndexed { idx, itm ->
                if (itm.id in listOf("item_user_01", "item_user_02")) {
                    inMemoryItems[idx] = itm.copy(ownerId = effectiveUserId)
                }
            }
            userItems = inMemoryItems.filter {
                it.ownerId == effectiveUserId && it.status == ItemStatus.DISPONIBLE && it.id != targetItemId
            }
        }

        setState {
            copy(
                targetItem = target,
                currentUserId = effectiveUserId,
                availableUserItems = userItems,
                selectedOfferedItemIds = emptySet(),
                truequeCoinsInput = "0",
                calculatedRatio = 0.0,
                isRatioValid = false,
                hasMinimumItemsSelected = false,
                canSubmitProposal = false,
                errorMessage = null
            )
        }

        // 4. Sincronizar en segundo plano si el artículo o nuevos ítems provienen de SQLite Room
        if (tradeItemDao != null) {
            viewModelScope.launch(dispatchers.io) {
                if (target == null) {
                    val dbTarget = tradeItemDao.getItemByIdSync(targetItemId)?.toDomainModel()
                    if (dbTarget != null) {
                        target = dbTarget
                        if (inMemoryItems.none { it.id == dbTarget.id }) inMemoryItems.add(dbTarget)
                        if (dbTarget.ownerId == effectiveUserId) {
                            withContext(dispatchers.main) {
                                setState {
                                    copy(
                                        targetItem = dbTarget,
                                        availableUserItems = emptyList(),
                                        canSubmitProposal = false,
                                        errorMessage = "Esta publicación es tuya. No puedes realizar un trueque contigo mismo."
                                    )
                                }
                            }
                            return@launch
                        }
                    }
                }

                val dbItems = tradeItemDao.getAllItemsSync()
                if (dbItems.isNotEmpty()) {
                    val mapped = dbItems.map { it.toDomainModel() }
                    mapped.forEach { m ->
                        val idx = inMemoryItems.indexOfFirst { it.id == m.id }
                        if (idx >= 0) inMemoryItems[idx] = m else inMemoryItems.add(m)
                    }
                    val updatedUserItems = inMemoryItems.filter {
                        it.ownerId == effectiveUserId && it.status == ItemStatus.DISPONIBLE && it.id != targetItemId
                    }
                    withContext(dispatchers.main) {
                        setState {
                            copy(
                                targetItem = target ?: targetItem,
                                availableUserItems = updatedUserItems
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toggleOfferedItem(itemId: String) {
        val currentSelected = currentState.selectedOfferedItemIds.toMutableSet()
        if (currentSelected.contains(itemId)) {
            currentSelected.remove(itemId)
        } else {
            currentSelected.add(itemId)
        }
        recalculateCreationValidation(currentSelected, currentState.truequeCoinsInput)
    }

    private fun updateTruequeCoins(coinsStr: String) {
        val sanitized = coinsStr.filter { it.isDigit() || it == '.' }
        recalculateCreationValidation(currentState.selectedOfferedItemIds, sanitized)
    }

    private fun recalculateCreationValidation(selectedItemIds: Set<String>, coinsStr: String) {
        val target = currentState.targetItem ?: return
        val coins = coinsStr.toDoubleOrNull() ?: 0.0
        val offeredItems = currentState.availableUserItems.filter { selectedItemIds.contains(it.id) }

        // Opción A: Debe seleccionar obligatoriamente al menos 1 artículo de su catálogo
        val hasMinItems = selectedItemIds.isNotEmpty()

        val ratio = try {
            if (offeredItems.isEmpty() && coins <= 0.0) {
                0.0
            } else {
                val targetMedian = target.estimatedValue.medianValue
                val offeredTotal = offeredItems.sumOf { it.estimatedValue.medianValue } + coins
                if (targetMedian > 0) offeredTotal / targetMedian else 0.0
            }
        } catch (e: Exception) {
            0.0
        }

        val isValidRatio = ratio >= ValidateTradeValueUseCase.MINIMUM_ACCEPTABLE_RATIO
        val canSubmit = hasMinItems && isValidRatio

        setState {
            copy(
                selectedOfferedItemIds = selectedItemIds,
                truequeCoinsInput = coinsStr,
                calculatedRatio = ratio,
                isRatioValid = isValidRatio,
                hasMinimumItemsSelected = hasMinItems,
                canSubmitProposal = canSubmit,
                errorMessage = null
            )
        }
    }

    private fun submitProposal() {
        val target = currentState.targetItem
        if (target == null) {
            setState { copy(errorMessage = "Artículo objetivo no encontrado.") }
            return
        }

        if (target.ownerId == currentState.currentUserId) {
            setState { copy(errorMessage = "Operación denegada: Un trueque requiere 2 partes distintas. No puedes ofertar por tu propio producto.") }
            return
        }

        if (!currentState.hasMinimumItemsSelected) {
            setState { copy(errorMessage = "Debes seleccionar al menos un artículo de tu catálogo (Opción A).") }
            return
        }

        val coins = currentState.truequeCoinsInput.toDoubleOrNull() ?: 0.0
        if (coins > currentState.truequeCoins) {
            setState { copy(errorMessage = "Saldo insuficiente en TruequeCoins.") }
            return
        }

        val offeredItems = currentState.availableUserItems.filter { currentState.selectedOfferedItemIds.contains(it.id) }

        try {
            // Validación canónica RN-02
            validateTradeValueUseCase(
                targetItem = target,
                offeredItems = offeredItems,
                compensatoryCoins = coins
            )

            val modality = when {
                coins > 0.0 && offeredItems.isNotEmpty() -> TradeModality.HIBRIDO_CON_CREDITOS
                coins > 0.0 -> TradeModality.PURO_TRUEQUECOINS
                offeredItems.size > 1 -> TradeModality.COMPUESTO_N_A_1
                else -> TradeModality.DIRECTO_1_A_1
            }

            val newProposal = TradeProposal(
                id = "prop_${System.currentTimeMillis()}",
                requesterUserId = currentState.currentUserId,
                receiverUserId = target.ownerId,
                offeredItemIds = offeredItems.map { it.id },
                targetItemId = target.id,
                compensatoryTruequeCoins = coins,
                modality = modality,
                status = TradeStatus.PENDIENTE
            )

            inMemoryProposals.add(newProposal)

            // Persistir la propuesta en SQLite Room
            viewModelScope.launch(dispatchers.io) {
                tradeProposalDao?.insertProposal(newProposal.toEntity())
            }

            sendEffect(TradeUiEffect.ShowToast("¡Propuesta enviada exitosamente!"))
            // Opción A: Redirección inmediata a la pantalla de negociación
            sendEffect(TradeUiEffect.NavigateToNegotiation(newProposal.id))
        } catch (e: BusinessRuleException.ValueRatioBelowThresholdException) {
            setState {
                copy(
                    errorMessage = "RN-02: La oferta combinada representa el ${String.format("%.1f", e.actualRatio * 100)}% del valor objetivo. El mínimo obligatorio es 40%."
                )
            }
        } catch (e: Exception) {
            setState { copy(errorMessage = e.message ?: "Error al enviar propuesta.") }
        }
    }

    // Listado de Propuestas (Pestañas 'Activas' y 'Ofertas')
    private fun loadProposals(userId: String) {
        // Activas: Trueques iniciados por el usuario pendientes de respuesta, o en estado RESERVADO / EN_ENTREGA
        val active = inMemoryProposals.filter {
            (it.requesterUserId == userId && it.status == TradeStatus.PENDIENTE) ||
            it.status in listOf(TradeStatus.RESERVADO, TradeStatus.EN_ENTREGA)
        }
        // Ofertas: Ofertas que otros usuarios te hacen respecto a tus artículos, o contraofertas recibidas sobre tu propuesta inicial
        val offers = inMemoryProposals.filter {
            (it.receiverUserId == userId && it.status in listOf(TradeStatus.PENDIENTE, TradeStatus.CONTRAOFERTADA)) ||
            (it.requesterUserId == userId && it.status == TradeStatus.CONTRAOFERTADA)
        }
        val history = inMemoryProposals.filter {
            (it.requesterUserId == userId || it.receiverUserId == userId) &&
            it.status in listOf(TradeStatus.COMPLETADO, TradeStatus.CANCELADO, TradeStatus.RECHAZADO, TradeStatus.EXPIRADO)
        }

        setState {
            copy(
                activeProposals = active,
                receivedOffers = offers,
                historyProposals = history,
                isLoading = false
            )
        }
    }

    private fun loadProposalDetails(proposalId: String, currentUserId: String) {
        val proposal = inMemoryProposals.find { it.id == proposalId }
        if (proposal == null) {
            setState { copy(errorMessage = "Propuesta no encontrada.") }
            return
        }

        val targetItem = inMemoryItems.find { it.id == proposal.targetItemId }
        val offeredItems = inMemoryItems.filter { proposal.offeredItemIds.contains(it.id) }

        // Si se encuentra en EN_ENTREGA, pre-crear o cargar cita en Safe Zone
        val appointment = if (proposal.status == TradeStatus.EN_ENTREGA || proposal.status == TradeStatus.RESERVADO) {
            MeetingAppointment(
                id = "meeting_${proposal.id}",
                tradeId = proposal.id,
                safeZone = SafeZonePoint(
                    id = "sz_coatza_forum",
                    name = "Punto Seguro Plaza Forum Coatzacoalcos",
                    addressDescription = "Av. Universidad Veracruzana 2601, domo central frente a vigilancia 24/7",
                    coordinates = GeoCoordinates(18.1415, -94.4852)
                ),
                scheduledTimeEpochMs = System.currentTimeMillis() + (2 * 60 * 60 * 1000L) // Cita en 2 horas (para prueba de RN-07)
            )
        } else null

        setState {
            copy(
                currentProposal = proposal,
                negotiationTargetItem = targetItem,
                negotiationOfferedItems = offeredItems,
                scheduledAppointment = appointment,
                counterOfferOfferedItemIds = proposal.offeredItemIds.toSet(),
                counterOfferTruequeCoinsInput = proposal.compensatoryTruequeCoins.toString()
            )
        }
    }

    // CU010: Aceptar Propuesta y Bloqueo Atómico ACID (RN-01)
    private fun acceptProposal(proposalId: String) {
        val proposal = inMemoryProposals.find { it.id == proposalId } ?: return
        val targetItem = inMemoryItems.find { it.id == proposal.targetItemId } ?: return
        val offeredItems = inMemoryItems.filter { proposal.offeredItemIds.contains(it.id) }

        try {
            val lockResult = lockInventoryForTradeUseCase(
                proposalToAccept = proposal,
                targetItem = targetItem,
                offeredItems = offeredItems,
                allActiveProposalsInSystem = inMemoryProposals
            )

            // Actualizar propuesta aceptada
            val index = inMemoryProposals.indexOfFirst { it.id == proposalId }
            if (index != -1) {
                inMemoryProposals[index] = lockResult.acceptedProposal
            }

            // Actualizar propuestas conflictivas canceladas automáticamente (RN-01)
            lockResult.affectedPendingProposalsToCancelOrPause.forEach { conflicting ->
                val cIndex = inMemoryProposals.indexOfFirst { it.id == conflicting.id }
                if (cIndex != -1) {
                    inMemoryProposals[cIndex] = conflicting
                }
            }

            // Actualizar artículos a RESERVADO en el inventario
            lockResult.lockedItems.forEach { lockedItem ->
                val itemIndex = inMemoryItems.indexOfFirst { it.id == lockedItem.id }
                if (itemIndex != -1) {
                    inMemoryItems[itemIndex] = lockedItem
                }
            }

            // Persistir aceptación y bloqueo en SQLite Room
            viewModelScope.launch(dispatchers.io) {
                tradeProposalDao?.updateProposal(lockResult.acceptedProposal.toEntity())
                lockResult.affectedPendingProposalsToCancelOrPause.forEach { conflicting ->
                    tradeProposalDao?.updateProposal(conflicting.toEntity())
                }
                lockResult.lockedItems.forEach { lockedItem ->
                    tradeItemDao?.updateItem(lockedItem.toEntity())
                }
            }

            setState {
                copy(
                    currentProposal = lockResult.acceptedProposal,
                    negotiationTargetItem = targetItem.copy(status = ItemStatus.RESERVADO),
                    negotiationOfferedItems = offeredItems.map { it.copy(status = ItemStatus.RESERVADO) }
                )
            }

            sendEffect(TradeUiEffect.ShowToast("¡Propuesta aceptada! Inventario asegurado en estado RESERVADO."))
        } catch (e: BusinessRuleException.ExclusivityLockException) {
            setState { copy(errorMessage = "Error RN-01: ${e.message}") }
        } catch (e: Exception) {
            setState { copy(errorMessage = e.message ?: "No fue posible aceptar la propuesta.") }
        }
    }

    // CU011: Emitir Contraoferta
    private fun toggleCounterOfferItem(itemId: String) {
        val updated = currentState.counterOfferOfferedItemIds.toMutableSet()
        if (updated.contains(itemId)) {
            updated.remove(itemId)
        } else {
            updated.add(itemId)
        }
        setState { copy(counterOfferOfferedItemIds = updated) }
    }

    private fun updateCounterOfferCoins(coinsStr: String) {
        val sanitized = coinsStr.filter { it.isDigit() || it == '.' }
        setState { copy(counterOfferTruequeCoinsInput = sanitized) }
    }

    private fun submitCounterOffer() {
        val proposal = currentState.currentProposal ?: return
        val target = currentState.negotiationTargetItem ?: return
        val newCoins = currentState.counterOfferTruequeCoinsInput.toDoubleOrNull() ?: 0.0
        val newOfferedItems = inMemoryItems.filter { currentState.counterOfferOfferedItemIds.contains(it.id) }

        try {
            validateTradeValueUseCase(target, newOfferedItems, newCoins)

            val updatedProposal = proposal.copy(
                offeredItemIds = newOfferedItems.map { it.id },
                compensatoryTruequeCoins = newCoins,
                status = TradeStatus.CONTRAOFERTADA,
                parentProposalId = proposal.id
            )

            val index = inMemoryProposals.indexOfFirst { it.id == proposal.id }
            if (index != -1) {
                inMemoryProposals[index] = updatedProposal
            }

            // Persistir contraoferta en SQLite Room
            viewModelScope.launch(dispatchers.io) {
                tradeProposalDao?.updateProposal(updatedProposal.toEntity())
            }

            setState {
                copy(
                    currentProposal = updatedProposal,
                    negotiationOfferedItems = newOfferedItems,
                    showCounterOfferDialog = false
                )
            }

            sendEffect(TradeUiEffect.ShowToast("Contraoferta enviada correctamente (CU011)."))
        } catch (e: BusinessRuleException.ValueRatioBelowThresholdException) {
            setState {
                copy(
                    errorMessage = "RN-02: La contraoferta no alcanza el 40% requerido (${String.format("%.1f", e.actualRatio * 100)}%)."
                )
            }
        }
    }

    // CU012: Rechazar propuesta
    private fun rejectProposal(proposalId: String) {
        val index = inMemoryProposals.indexOfFirst { it.id == proposalId }
        if (index != -1) {
            val rejected = inMemoryProposals[index].copy(status = TradeStatus.RECHAZADO)
            inMemoryProposals[index] = rejected
            viewModelScope.launch(dispatchers.io) {
                tradeProposalDao?.updateProposal(rejected.toEntity())
            }
            setState { copy(currentProposal = rejected) }
            sendEffect(TradeUiEffect.ShowToast("Propuesta rechazada formalmente."))
            sendEffect(TradeUiEffect.NavigateBack)
        }
    }

    // CU012: Cancelar propuesta evaluando Ventana de 4 Horas (RN-07)
    private fun requestCancelProposal(proposalId: String) {
        val proposal = inMemoryProposals.find { it.id == proposalId } ?: return
        val appointment = currentState.scheduledAppointment

        if (appointment != null && currentState.currentUserProfile != null) {
            val evaluation = evaluateCancellationWindowUseCase(
                cancelingUser = currentState.currentUserProfile!!,
                appointment = appointment
            )

            setState {
                copy(
                    showCancelDialog = true,
                    isPenaltyCancellation = evaluation.isPenaltyApplied,
                    cancellationWarningMessage = evaluation.message
                )
            }
        } else {
            setState {
                copy(
                    showCancelDialog = true,
                    isPenaltyCancellation = false,
                    cancellationWarningMessage = "¿Estás seguro de cancelar este intercambio?"
                )
            }
        }
    }

    private fun confirmCancelProposal() {
        val proposal = currentState.currentProposal ?: return
        val appointment = currentState.scheduledAppointment

        // Si aplica penalización RN-07, actualizar la tasa de cumplimiento
        if (currentState.isPenaltyCancellation && appointment != null && currentState.currentUserProfile != null) {
            val evaluation = evaluateCancellationWindowUseCase(
                cancelingUser = currentState.currentUserProfile!!,
                appointment = appointment
            )
            setState { copy(currentUserProfile = evaluation.updatedUserProfile) }
        }

        // Liberar artículos reservados si existían
        val targetItem = currentState.negotiationTargetItem
        if (targetItem?.status == ItemStatus.RESERVADO) {
            val tIndex = inMemoryItems.indexOfFirst { it.id == targetItem.id }
            if (tIndex != -1) inMemoryItems[tIndex] = targetItem.copy(status = ItemStatus.DISPONIBLE)
        }
        currentState.negotiationOfferedItems.forEach { offered ->
            if (offered.status == ItemStatus.RESERVADO) {
                val oIndex = inMemoryItems.indexOfFirst { it.id == offered.id }
                if (oIndex != -1) inMemoryItems[oIndex] = offered.copy(status = ItemStatus.DISPONIBLE)
            }
        }

        val updatedProposal = proposal.copy(status = TradeStatus.CANCELADO)
        val pIndex = inMemoryProposals.indexOfFirst { it.id == proposal.id }
        if (pIndex != -1) {
            inMemoryProposals[pIndex] = updatedProposal
        }

        viewModelScope.launch(dispatchers.io) {
            tradeProposalDao?.updateProposal(updatedProposal.toEntity())
            if (targetItem?.status == ItemStatus.RESERVADO) {
                tradeItemDao?.updateItem(targetItem.copy(status = ItemStatus.DISPONIBLE).toEntity())
            }
        }

        setState {
            copy(
                currentProposal = updatedProposal,
                showCancelDialog = false,
                cancellationWarningMessage = null
            )
        }

        sendEffect(TradeUiEffect.ShowToast("Propuesta cancelada."))
        sendEffect(TradeUiEffect.NavigateBack)
    }

    // Helper público para pruebas unitarias directas
    fun addTestProposal(proposal: TradeProposal) {
        inMemoryProposals.add(proposal)
    }

    fun addTestItem(item: TradeItem) {
        inMemoryItems.add(item)
    }
}
