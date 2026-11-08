package com.truequapp.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Agregado que une el contrato consolidado de Trueque con sus dos ofertas componentes (Oferta A y Oferta B).
 */
data class TruequeConOfertas(
    @Embedded
    val trueque: TruequeEntity,

    @Relation(
        parentColumn = "oferta_a_id",
        entityColumn = "id"
    )
    val ofertaA: OfertaItemEntity?,

    @Relation(
        parentColumn = "oferta_b_id",
        entityColumn = "id"
    )
    val ofertaB: OfertaItemEntity?
) {
    /**
     * Variable directa que devuelve si la propuesta está activa (True) o no (False).
     */
    val estaActiva: Boolean get() = trueque.estaActiva
}

