package com.truequapp.feature.trade.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.database.repository.UserRepository
import com.truequapp.core.database.storage.UserPublicationStorage
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
import com.truequapp.core.domain.rules.BusinessRuleException
import com.truequapp.core.domain.rules.EvaluateCancellationWindowUseCase
import com.truequapp.core.domain.rules.LockInventoryForTradeUseCase
import com.truequapp.core.domain.rules.ValidateTradeValueUseCase
import com.truequapp.feature.trade.data.TradeRepository
import com.truequapp.feature.trade.data.toTradeProposal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel que orquesta el Motor de Propuestas de Trueque de la Fase 2 (CU009, CU010, CU011, CU012)
 * y la gestión de "Mis Publicaciones" (eliminación y cancelación) trasladadas a la pantalla de Trueques.
 * Aplica con rigor las reglas de negocio canónicas:
 * - RN-01: Exclusividad y bloqueo atómico ACID de inventario.
 * - RN-02: Umbral mínimo de valor del 40% (Banda de equivalencia).
 * - RN-07: Ventana de cancelación y penalización de asistencia ante imprevistos (< 4h).
 * - Persistencia local y consolidación de Oferta A, Oferta B y Trueque con flag 'esta_activa' (SQL Room).
 */
@HiltViewModel
class TradeViewModel @Inject constructor(
    private val validateTradeValueUseCase: ValidateTradeValueUseCase = ValidateTradeValueUseCase(),
    private val lockInventoryForTradeUseCase: LockInventoryForTradeUseCase = LockInventoryForTradeUseCase(),
    private val evaluateCancellationWindowUseCase: EvaluateCancellationWindowUseCase = EvaluateCancellationWindowUseCase(),
    private val userSessionDataStore: UserSessionDataStore,
    private val dispatchers: DispatcherProvider,
    private val tradeRepository: TradeRepository? = null,
    private val userPublicationStorage: UserPublicationStorage? = null,
    private val userRepository: UserRepository? = null
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
                id = "item_chocolatt_01",
                ownerId = "usr_chocolatt",
                title = "Laptop Dell Inspiron Core i5 para Programación (CETIS 79)",
                description = "Laptop Dell con Intel Core i5, 16GB RAM y SSD 512GB. Ideal para talleres de programación y ofimática en CETIS 79.",
                categoryId = 2,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 5500.0,
                estimatedValueMax = 6500.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1496181133206-80ce9b88a853"),
                tagsWishlist = listOf("Calculadora Científica", "Mochila Escolar", "Kit Electrónica"),
                isAvailable = true
            ),
            TradeItem(
                id = "item_chocolatt_02",
                ownerId = "usr_chocolatt",
                title = "Bolígrafo con Diseño Especial Escolar",
                description = "Bolígrafo metálico recargable con diseño ergonómico de alta precisión para apuntes técnicos y exámenes.",
                categoryId = 3,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 150.0,
                estimatedValueMax = 250.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1583485088034-697b5bc54ccd"),
                tagsWishlist = listOf("Calculadora", "Mochila", "Cuadernos"),
                isAvailable = true
            ),
            TradeItem(
                id = "item_vergara_01",
                ownerId = "usr_vergara",
                title = "Mochila Escolar Reforzada CETIS 79",
                description = "Mochila ergonómica de alta resistencia con compartimento acolchado para laptop de 15.6 pulgadas y puerto USB.",
                categoryId = 4,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 650.0,
                estimatedValueMax = 850.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1553062407-98eeb64c6a62"),
                tagsWishlist = listOf("Laptop Dell", "Bolígrafo con diseño", "Calculadora"),
                isAvailable = true
            ),
            TradeItem(
                id = "item_vergara_02",
                ownerId = "usr_vergara",
                title = "Calculadora Científica Casio fx-991LA Plus",
                description = "Calculadora científica oficial requerida para cálculo diferencial, integral y especialidades técnicas CETIS 79.",
                categoryId = 3,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 650.0,
                estimatedValueMax = 850.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1578303512597-81e6cc155b3e"),
                tagsWishlist = listOf("Laptop Dell", "Bolígrafo con diseño", "Mochila"),
                isAvailable = true
            ),
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

        // Sincronizar publicaciones del catálogo central (servidor simulado)
        val savedPublications = userPublicationStorage?.getRemoteCatalogItems()
            ?: userPublicationStorage?.loadPublishedItems().orEmpty()
        savedPublications.forEach { saved ->
            if (inMemoryItems.none { it.id == saved.id }) {
                inMemoryItems.add(saved)
            }
        }
    }

    private fun loadSessionUser() {
        viewModelScope.launch(dispatchers.io) {
            val session = userSessionDataStore.sessionFlow.first()
            val userId = session.userId ?: "usr_current"
            val userEmail = session.userEmail

            // Consultar datos del usuario desde la base de datos SQL (Single Source of Truth)
            val dbUser = if (!userEmail.isNullOrBlank()) {
                userRepository?.getCurrentUser()
            } else null

            val coinsFromDb = dbUser?.cantidadTruequeCoins
                ?: userRepository?.getCoins(userEmail)
                ?: 100.0

            val kyc = if (session.isKycVerified) KycLevel.TELEFONO_SMS_NIVEL_1 else KycLevel.BASICO_NIVEL_0
            val userProfile = UserProfile(
                id = userId,
                fullName = dbUser?.usuario ?: "Usuario Truequapp",
                username = dbUser?.usuario ?: "truequero_${userId.takeLast(4)}",
                email = userEmail ?: "user_${userId}@truequapp.com",
                phoneNumber = dbUser?.numeroTelefonico ?: "+525500000000",
                kycLevel = kyc,
                attendanceRate = 1.0,
                location = ObfuscatedGeoLocation(
                    neighborhoodName = "Zona Universitaria",
                    approximateCoordinates = GeoCoordinates(19.4326, -99.1332)
                )
            )

            loadUserPublications(userId)

            if (!userEmail.isNullOrBlank()) {
                tradeRepository?.syncProposalsFromCloud(userEmail)
            }

            setState {
                copy(
                    currentUserId = userId,
                    truequeCoins = coinsFromDb,
                    currentUserProfile = userProfile
                )
            }

            // Observar en tiempo real las coins desde la base de datos
            userRepository?.observeCoins(userEmail)?.collectLatest { realTimeCoins ->
                setState { copy(truequeCoins = realTimeCoins) }
            }
        }
    }

    private fun isMyTradeItem(item: TradeItem, userId: String): Boolean {
        val email = currentState.currentUserProfile?.email
        return item.ownerId.equals(userId, ignoreCase = true) || 
               (email != null && item.ownerId.equals(email, ignoreCase = true)) || 
               (userId == "usr_current" && item.ownerId == "usr_current")
    }

    private fun loadUserPublications(userId: String) {
        val activeSessionEmail = currentState.currentUserProfile?.email
        val effectiveUserId = if (userId.isNotBlank() && userId != "usuario_actual") {
            userId
        } else {
            activeSessionEmail ?: currentState.currentUserId.takeIf { it != "usuario_actual" } ?: "usr_current"
        }

        // Cargar exclusivamente las publicaciones del usuario desde su archivo local
        val userSpecific = userPublicationStorage?.loadUserPublications(effectiveUserId).orEmpty()
        val userItems = if (userSpecific.isNotEmpty()) {
            userSpecific.filter { isMyTradeItem(it, effectiveUserId) }
        } else {
            // Fallback al catálogo si el usuario aún no tiene archivo local
            val all = userPublicationStorage?.getRemoteCatalogItems()
                ?: userPublicationStorage?.loadPublishedItems().orEmpty()
            val fromAll = all.filter { isMyTradeItem(it, effectiveUserId) }
            if (fromAll.isNotEmpty()) {
                userPublicationStorage?.saveUserPublications(effectiveUserId, fromAll)
                fromAll
            } else {
                inMemoryItems.filter { isMyTradeItem(it, effectiveUserId) }
            }
        }.distinctBy { it.id }

        setState { copy(myPublications = userItems) }
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
            is TradeUiEvent.OpenOfferDetail -> openOfferDetail(event.proposalId)
            is TradeUiEvent.CloseOfferDetail -> setState {
                copy(
                    selectedOfferProposal = null,
                    selectedOfferProponentUser = null,
                    selectedOfferTargetItem = null,
                    selectedOfferOfferedItems = emptyList()
                )
            }
            is TradeUiEvent.RefreshMyPublications -> loadUserPublications(currentState.currentUserId)
            is TradeUiEvent.DeletePublication -> deletePublication(event.itemId)
            is TradeUiEvent.TogglePausePublication -> togglePausePublication(event.itemId)
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

    // Gestión de Mis Publicaciones trasladadas a Trueques
    private fun deletePublication(itemId: String) {
        val item = inMemoryItems.find { it.id == itemId }
            ?: currentState.myPublications.find { it.id == itemId }

        if (item != null && item.status == ItemStatus.RESERVADO) {
            setState {
                copy(errorMessage = "RN-01: No se puede eliminar un artículo reservado en una transacción activa.")
            }
            return
        }

        inMemoryItems.removeIf { it.id == itemId }
        val activeSessionEmail = currentState.currentUserProfile?.email
        val effectiveUserId = activeSessionEmail ?: currentState.currentUserId

        // Eliminar del servidor central y del archivo local del usuario
        userPublicationStorage?.deleteItemFromServer(itemId)
        userPublicationStorage?.deleteUserPublication(effectiveUserId, itemId)

        val updatedPublications = currentState.myPublications.filter { it.id != itemId }
        val updatedAvailable = currentState.availableUserItems.filter { it.id != itemId }

        setState {
            copy(
                myPublications = updatedPublications,
                availableUserItems = updatedAvailable
            )
        }

        sendEffect(TradeUiEffect.ShowToast("Publicación eliminada correctamente."))
    }

    private fun togglePausePublication(itemId: String) {
        val item = inMemoryItems.find { it.id == itemId }
            ?: currentState.myPublications.find { it.id == itemId }

        if (item == null) {
            setState { copy(errorMessage = "Publicación no encontrada.") }
            return
        }

        if (item.status == ItemStatus.RESERVADO || item.status == ItemStatus.EN_ENTREGA) {
            setState {
                copy(errorMessage = "RN-01: No se puede modificar el estado de un artículo con negociación activa.")
            }
            return
        }

        val newStatus = if (item.status == ItemStatus.PAUSADO) ItemStatus.DISPONIBLE else ItemStatus.PAUSADO
        val isNowAvailable = newStatus == ItemStatus.DISPONIBLE
        val updatedItem = item.copy(status = newStatus)

        // Actualizar en memoria
        val memIndex = inMemoryItems.indexOfFirst { it.id == itemId }
        if (memIndex != -1) {
            inMemoryItems[memIndex] = updatedItem
        } else {
            inMemoryItems.add(updatedItem)
        }

        val activeSessionEmail = currentState.currentUserProfile?.email
        val effectiveUserId = activeSessionEmail ?: currentState.currentUserId

        // Persistir en almacenamiento del servidor y local del usuario
        userPublicationStorage?.updateItemOnServer(updatedItem)
        userPublicationStorage?.updateUserPublication(effectiveUserId, updatedItem)

        val updatedPublications = currentState.myPublications.map {
            if (it.id == itemId) updatedItem else it
        }

        val updatedAvailable = if (isNowAvailable) {
            if (currentState.availableUserItems.none { it.id == itemId }) {
                currentState.availableUserItems + updatedItem
            } else {
                currentState.availableUserItems.map { if (it.id == itemId) updatedItem else it }
            }
        } else {
            currentState.availableUserItems.filter { it.id != itemId }
        }

        setState {
            copy(
                myPublications = updatedPublications,
                availableUserItems = updatedAvailable
            )
        }

        val toastMsg = if (isNowAvailable) {
            "Publicación reactivada y visible en el catálogo."
        } else {
            "Publicación pausada (oculta temporalmente del catálogo)."
        }
        sendEffect(TradeUiEffect.ShowToast(toastMsg))
    }

    // CU009: Proponer Trueque Directo o Híbrido
    private fun initTradeCreation(targetItemId: String, userId: String) {
        // Consultar el catálogo del servidor central remoto simulado
        val remoteCatalog = userPublicationStorage?.getRemoteCatalogItems()
            ?: userPublicationStorage?.loadPublishedItems().orEmpty()
        val allCatalogItems = (inMemoryItems + remoteCatalog).distinctBy { it.id }
        val target = allCatalogItems.find { it.id == targetItemId }

        val activeSessionEmail = currentState.currentUserProfile?.email
        val effectiveUserId = if (userId.isNotBlank() && userId != "usuario_actual") {
            userId
        } else {
            activeSessionEmail ?: currentState.currentUserId.takeIf { it != "usuario_actual" } ?: "usr_current"
        }

        // Anti-auto-trueque: no puedes ofertar ni realizar un intercambio sobre tu propio artículo
        if (target != null && (target.ownerId == effectiveUserId || isMyTradeItem(target, effectiveUserId))) {
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

        // Cargar los artículos del usuario para ofertar desde su archivo local exclusivo
        // REGLA: Solo se pueden seleccionar publicaciones que cumplan `it.isAvailable` (status == ItemStatus.DISPONIBLE)
        val userSpecificItems = userPublicationStorage?.loadUserPublications(effectiveUserId).orEmpty()
        val userItems = if (userSpecificItems.isNotEmpty()) {
            userSpecificItems.filter { isMyTradeItem(it, effectiveUserId) && it.isAvailable }
        } else {
            allCatalogItems.filter { isMyTradeItem(it, effectiveUserId) && it.isAvailable }
        }.distinctBy { it.id }

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
    }

    private fun toggleOfferedItem(itemId: String) {
        val targetOfferItem = currentState.availableUserItems.find { it.id == itemId }
        // Si el artículo no está disponible o no es seleccionable para trueque, se ignora la selección
        if (targetOfferItem != null && !targetOfferItem.isAvailable) {
            setState {
                copy(errorMessage = "El artículo '${targetOfferItem.title}' no está disponible para intercambios.")
            }
            return
        }

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

            // Persistencia en SQLite Room conforme al boceto (Oferta A, Oferta B y Trueque con esta_activa = true)
            val userEmail = currentState.currentUserProfile?.email ?: currentState.currentUserId
            viewModelScope.launch(dispatchers.io) {
                try {
                    val targetEmail = if (target.ownerId.contains("@")) target.ownerId else {
                        userRepository?.getUserByEmail(target.ownerId)?.correo ?: target.ownerId
                    }
                    tradeRepository?.saveProposal(
                        proposal = newProposal,
                        userACorreo = userEmail,
                        userBCorreo = targetEmail
                    )
                    // Si se ofertaron coins compensatorias, reducirlas en la base de datos (Single Source of Truth)
                    if (coins > 0.0) {
                        userRepository?.reduceCoins(coins, userEmail)
                    }
                } catch (e: Exception) {
                    // Silencioso o log de debug
                }
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

    // Listado de Propuestas y Publicaciones (Pestañas 'Activas', 'Ofertas' y 'Mis Publicaciones')
    private fun loadProposals(userId: String) {
        val userEmail = currentState.currentUserProfile?.email ?: userId

        viewModelScope.launch(dispatchers.io) {
            // Sincronizar propuestas desde la nube (Turso) antes de leer la base de datos local
            try {
                if (userEmail.isNotBlank()) {
                    tradeRepository?.syncProposalsFromCloud(userEmail)
                }
            } catch (_: Exception) {
            }

            // Sincronizar desde la base de datos SQL local si existe el repositorio
            try {
                val dbProposalsEmail = tradeRepository?.getActiveProposalsForUser(userEmail)?.firstOrNull().orEmpty()
                val dbProposalsId = if (!userId.equals(userEmail, ignoreCase = true)) {
                    tradeRepository?.getActiveProposalsForUser(userId)?.firstOrNull().orEmpty()
                } else emptyList()

                (dbProposalsEmail + dbProposalsId).forEach { activeTrueque ->
                    val domainProp = activeTrueque.toTradeProposal()
                    val idx = inMemoryProposals.indexOfFirst { it.id == domainProp.id }
                    if (idx == -1) {
                        inMemoryProposals.add(domainProp)
                    } else {
                        inMemoryProposals[idx] = domainProp
                    }
                }
            } catch (e: Exception) {
                // Continuar en caso de entorno de prueba sin BD
            }

            // Sincronizar publicaciones del usuario
            loadUserPublications(userId)

            fun isReceiver(prop: TradeProposal): Boolean {
                return prop.receiverUserId.equals(userEmail, ignoreCase = true) ||
                       prop.receiverUserId.equals(userId, ignoreCase = true) ||
                       (userEmail?.contains("chocolatt", ignoreCase = true) == true && prop.receiverUserId.contains("chocolatt", ignoreCase = true)) ||
                       (userEmail?.contains("vergara", ignoreCase = true) == true && prop.receiverUserId.contains("vergara", ignoreCase = true)) ||
                       (userId.contains("chocolatt", ignoreCase = true) && prop.receiverUserId.contains("chocolatt", ignoreCase = true)) ||
                       (userId.contains("vergara", ignoreCase = true) && prop.receiverUserId.contains("vergara", ignoreCase = true)) ||
                       (userId == "usr_current" && prop.receiverUserId == "usr_current")
            }

            fun isRequester(prop: TradeProposal): Boolean {
                return prop.requesterUserId.equals(userEmail, ignoreCase = true) ||
                       prop.requesterUserId.equals(userId, ignoreCase = true) ||
                       (userEmail?.contains("chocolatt", ignoreCase = true) == true && prop.requesterUserId.contains("chocolatt", ignoreCase = true)) ||
                       (userEmail?.contains("vergara", ignoreCase = true) == true && prop.requesterUserId.contains("vergara", ignoreCase = true)) ||
                       (userId.contains("chocolatt", ignoreCase = true) && prop.requesterUserId.contains("chocolatt", ignoreCase = true)) ||
                       (userId.contains("vergara", ignoreCase = true) && prop.requesterUserId.contains("vergara", ignoreCase = true)) ||
                       (userId == "usr_current" && prop.requesterUserId == "usr_current")
            }

            // Activas: Trueques iniciados por el usuario pendientes de respuesta, o en estado RESERVADO / EN_ENTREGA
            val active = inMemoryProposals.filter {
                (isRequester(it) && it.status == TradeStatus.PENDIENTE) ||
                it.status in listOf(TradeStatus.RESERVADO, TradeStatus.EN_ENTREGA)
            }
            // Ofertas: Ofertas que otros usuarios te hacen respecto a tus artículos, o contraofertas recibidas sobre tu propuesta inicial
            val offers = inMemoryProposals.filter {
                (isReceiver(it) && it.status in listOf(TradeStatus.PENDIENTE, TradeStatus.CONTRAOFERTADA)) ||
                (isRequester(it) && it.status == TradeStatus.CONTRAOFERTADA)
            }
            val history = inMemoryProposals.filter {
                (isRequester(it) || isReceiver(it)) &&
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
    }

    private fun openOfferDetail(proposalId: String) {
        val proposal = inMemoryProposals.find { it.id == proposalId } ?: return
        val remoteCatalog = userPublicationStorage?.getRemoteCatalogItems()
            ?: userPublicationStorage?.loadPublishedItems().orEmpty()
        val allItems = (inMemoryItems + remoteCatalog).distinctBy { it.id }
        val targetItem = allItems.find { it.id == proposal.targetItemId }
        val offeredItems = allItems.filter { proposal.offeredItemIds.contains(it.id) }

        viewModelScope.launch(dispatchers.io) {
            val proponent = userRepository?.getUserByEmail(proposal.requesterUserId)
            setState {
                copy(
                    selectedOfferProposal = proposal,
                    selectedOfferProponentUser = proponent,
                    selectedOfferTargetItem = targetItem,
                    selectedOfferOfferedItems = offeredItems
                )
            }
        }
    }

    private fun loadProposalDetails(proposalId: String, currentUserId: String) {
        val proposal = inMemoryProposals.find { it.id == proposalId }
        if (proposal == null) {
            setState { copy(errorMessage = "Propuesta no encontrada.") }
            return
        }

        val remoteCatalog = userPublicationStorage?.getRemoteCatalogItems()
            ?: userPublicationStorage?.loadPublishedItems().orEmpty()
        val allItems = (inMemoryItems + remoteCatalog).distinctBy { it.id }
        val targetItem = allItems.find { it.id == proposal.targetItemId }
        val offeredItems = allItems.filter { proposal.offeredItemIds.contains(it.id) }

        // Si se encuentra en EN_ENTREGA, pre-crear o cargar cita en Safe Zone
        val appointment = if (proposal.status == TradeStatus.EN_ENTREGA || proposal.status == TradeStatus.RESERVADO) {
            MeetingAppointment(
                id = "meeting_${proposal.id}",
                tradeId = proposal.id,
                safeZone = SafeZonePoint(
                    id = "sz_01",
                    name = "Punto Seguro Estación Central",
                    addressDescription = "Módulo de vigilancia y videocámaras 24/7",
                    coordinates = GeoCoordinates(19.4326, -99.1332)
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
        val remoteCatalog = userPublicationStorage?.getRemoteCatalogItems()
            ?: userPublicationStorage?.loadPublishedItems().orEmpty()
        val allItems = (inMemoryItems + remoteCatalog).distinctBy { it.id }
        val targetItem = allItems.find { it.id == proposal.targetItemId } ?: inMemoryItems.find { it.id == proposal.targetItemId } ?: return
        val offeredItems = allItems.filter { proposal.offeredItemIds.contains(it.id) }

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
                userPublicationStorage?.updateItemOnServer(lockedItem)
            }

            // Persistir aceptación en base de datos (pasa a RESERVADO y mantiene esta_activa = true)
            viewModelScope.launch(dispatchers.io) {
                try {
                    tradeRepository?.updateProposalStatus(proposalId, TradeStatus.RESERVADO)
                    lockResult.affectedPendingProposalsToCancelOrPause.forEach { conflicting ->
                        tradeRepository?.updateProposalStatus(conflicting.id, TradeStatus.CANCELADO)
                    }
                } catch (e: Exception) {
                    // Silencioso o log
                }
            }

            setState {
                copy(
                    currentProposal = lockResult.acceptedProposal,
                    negotiationTargetItem = targetItem.copy(status = ItemStatus.RESERVADO),
                    negotiationOfferedItems = offeredItems.map { it.copy(status = ItemStatus.RESERVADO) }
                )
            }

            sendEffect(TradeUiEffect.ShowToast("¡Propuesta aceptada! El chat ha sido habilitado."))
            sendEffect(TradeUiEffect.NavigateToChat(proposal.id))
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
        if (userRepository != null && newCoins > currentState.truequeCoins) {
            setState { copy(errorMessage = "Saldo insuficiente en TruequeCoins para emitir esta contraoferta.") }
            return
        }
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

            // Persistir contraoferta en la base de datos (estado CONTRAOFERTADA con esta_activa = true)
            viewModelScope.launch(dispatchers.io) {
                try {
                    val targetEmail = if (target.ownerId.contains("@")) target.ownerId else {
                        userRepository?.getUserByEmail(target.ownerId)?.correo ?: target.ownerId
                    }
                    tradeRepository?.submitCounterOffer(
                        proposalId = proposal.id,
                        newOfferedItemIds = newOfferedItems.map { it.id },
                        newTruequeCoins = newCoins
                    )
                } catch (e: Exception) {
                    // Silencioso o log
                }
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
            setState { copy(currentProposal = rejected) }

            // Persistir rechazo en base de datos (pasa a esta_activa = false)
            viewModelScope.launch(dispatchers.io) {
                try {
                    tradeRepository?.updateProposalStatus(proposalId, TradeStatus.RECHAZADO)
                } catch (e: Exception) {
                    // Silencioso o log
                }
            }

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

        // Persistir cancelación en base de datos (pasa a esta_activa = false)
        viewModelScope.launch(dispatchers.io) {
            try {
                tradeRepository?.updateProposalStatus(proposal.id, TradeStatus.CANCELADO)
            } catch (e: Exception) {
                // Silencioso o log
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
