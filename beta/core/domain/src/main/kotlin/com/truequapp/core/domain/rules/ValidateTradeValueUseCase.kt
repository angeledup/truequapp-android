package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.TradeItem

/**
 * Caso de uso: Validación de la Banda Mínima de Equivalencia de Valor (RN-02).
 * Previene spam y ofertas abusivas en el motor de negociación.
 */
class ValidateTradeValueUseCase {

    /**
     * Valida si una propuesta cumple con el umbral mínimo del 40% del valor del bien objetivo.
     *
     * @param targetItem Artículo objetivo que se desea obtener.
     * @param offeredItems Lista de 1 a N artículos propios ofertados.
     * @param truequeCoins Créditos adicionales en TruequeCoins ofertados como diferencial (moneda única).
     * @param compensatoryCoins Alias retrocompatible para truequeCoins.
     * @return El porcentaje calculado de valor de la propuesta (ej. 0.85 = 85%).
     * @throws BusinessRuleException.ValueRatioBelowThresholdException si la oferta es menor al 40%.
     */
    operator fun invoke(
        targetItem: TradeItem,
        offeredItems: List<TradeItem>,
        truequeCoins: Double = 0.0,
        compensatoryCoins: Double = truequeCoins
    ): Double {
        val coins = if (truequeCoins > 0.0) truequeCoins else compensatoryCoins
        require(offeredItems.isNotEmpty() || coins > 0.0) {
            "Una oferta debe incluir al menos un artículo o créditos en TruequeCoins."
        }
        require(coins >= 0.0) {
            "Los créditos compensatorios no pueden ser negativos."
        }

        val targetMedianValue = targetItem.estimatedValue.medianValue
        require(targetMedianValue > 0.0) {
            "El artículo objetivo debe tener un valor estimado positivo."
        }

        val offeredItemsTotalValue = offeredItems.sumOf { it.estimatedValue.medianValue }
        val totalCombinedOfferValue = offeredItemsTotalValue + coins

        val valueRatio = totalCombinedOfferValue / targetMedianValue

        if (valueRatio < MINIMUM_ACCEPTABLE_RATIO) {
            throw BusinessRuleException.ValueRatioBelowThresholdException(
                actualRatio = valueRatio,
                requiredRatio = MINIMUM_ACCEPTABLE_RATIO
            )
        }

        return valueRatio
    }

    companion object {
        const val MINIMUM_ACCEPTABLE_RATIO = 0.40 // 40% según RN-02 de la UV
    }
}
