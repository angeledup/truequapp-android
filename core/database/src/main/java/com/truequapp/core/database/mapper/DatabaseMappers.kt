package com.truequapp.core.database.mapper

import com.truequapp.core.database.entity.TradeItemEntity
import com.truequapp.core.database.entity.TradeProposalEntity
import com.truequapp.core.domain.model.EstimatedValueRange
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import org.json.JSONArray

fun parseJsonStringList(jsonStr: String?): List<String> {
    if (jsonStr.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

fun toJsonString(list: List<String>): String {
    val array = JSONArray()
    list.forEach { array.put(it) }
    return array.toString()
}

fun TradeItemEntity.toDomainModel(): TradeItem {
    val safeType = runCatching { ItemType.valueOf(type) }.getOrDefault(ItemType.BIEN_FISICO)
    val safeCondition = runCatching { ItemCondition.valueOf(condition) }.getOrDefault(ItemCondition.USADO)
    val safeStatus = runCatching { ItemStatus.valueOf(status) }.getOrDefault(ItemStatus.DISPONIBLE)

    return TradeItem(
        id = id,
        ownerId = ownerId,
        title = title,
        description = description,
        categoryId = categoryId,
        type = safeType,
        condition = safeCondition,
        estimatedValue = EstimatedValueRange(estimatedValueMin, estimatedValueMax),
        imagesWebP = parseJsonStringList(imagesWebPJson),
        tagsWishlist = parseJsonStringList(tagsWishlistJson),
        status = safeStatus,
        createdAtEpochMs = createdAtEpochMs
    )
}

fun TradeItem.toEntity(
    ownerName: String = "Usuario Truequapp",
    neighborhood: String = "Coatzacoalcos, Ver.",
    city: String = "Coatzacoalcos"
): TradeItemEntity {
    return TradeItemEntity(
        id = id,
        ownerId = ownerId,
        ownerName = ownerName,
        title = title,
        description = description,
        categoryId = categoryId,
        condition = condition.name,
        type = type.name,
        estimatedValueMin = estimatedValueMin,
        estimatedValueMax = estimatedValueMax,
        imagesWebPJson = toJsonString(imagesWebP),
        tagsWishlistJson = toJsonString(tagsWishlist),
        status = status.name,
        isBookmarked = false,
        locationNeighborhood = neighborhood,
        city = city,
        createdAtEpochMs = createdAtEpochMs
    )
}

fun TradeProposalEntity.toDomainModel(): TradeProposal {
    val offeredList = parseJsonStringList(offeredItemIdsJson)
    val safeModality = when {
        monetaryCompensationCoins > 0 && offeredList.isNotEmpty() -> TradeModality.HIBRIDO_CON_CREDITOS
        monetaryCompensationCoins > 0 -> TradeModality.PURO_TRUEQUECOINS
        offeredList.size > 1 -> TradeModality.COMPUESTO_N_A_1
        else -> TradeModality.DIRECTO_1_A_1
    }

    val safeStatus = when (status) {
        "CONTRAOFERTADA" -> TradeStatus.CONTRAOFERTADA
        "RESERVADO", "NEGOCIACION_ACTIVA" -> TradeStatus.RESERVADO
        "EN_ENTREGA", "CITA_PROGRAMADA" -> TradeStatus.EN_ENTREGA
        "COMPLETADO" -> TradeStatus.COMPLETADO
        "CANCELADO" -> TradeStatus.CANCELADO
        "RECHAZADO" -> TradeStatus.RECHAZADO
        "DISPUTA", "EN_DISPUTA" -> TradeStatus.EN_DISPUTA
        else -> TradeStatus.PENDIENTE
    }

    return TradeProposal(
        id = id,
        requesterUserId = initiatorUserId,
        receiverUserId = receiverUserId,
        offeredItemIds = offeredList,
        targetItemId = targetItemId,
        compensatoryTruequeCoins = monetaryCompensationCoins,
        modality = safeModality,
        status = safeStatus,
        createdAtEpochMs = createdAtEpochMs
    )
}

fun TradeProposal.toEntity(): TradeProposalEntity {
    val entityStatus = when (status) {
        TradeStatus.CONTRAOFERTADA -> "CONTRAOFERTADA"
        TradeStatus.RESERVADO -> "RESERVADO"
        TradeStatus.EN_ENTREGA -> "EN_ENTREGA"
        TradeStatus.COMPLETADO -> "COMPLETADO"
        TradeStatus.CANCELADO -> "CANCELADO"
        TradeStatus.RECHAZADO -> "RECHAZADO"
        TradeStatus.EN_DISPUTA -> "EN_DISPUTA"
        else -> "PROPUESTA_ENVIADA"
    }

    return TradeProposalEntity(
        id = id,
        initiatorUserId = requesterUserId,
        receiverUserId = receiverUserId,
        targetItemId = targetItemId,
        offeredItemIdsJson = toJsonString(offeredItemIds),
        monetaryCompensationCoins = compensatoryTruequeCoins,
        status = entityStatus,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = System.currentTimeMillis()
    )
}

