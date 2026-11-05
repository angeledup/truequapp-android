package com.truequapp.core.domain.model

/**
 * Condición física del artículo o servicio ofertado.
 */
enum class ItemCondition(val displayName: String) {
    NUEVO("Nuevo en caja / empaque original"),
    COMO_NUEVO("Como nuevo / sin detalles"),
    USADO("Usado en buen estado"),
    REGULAR("Con detalles estéticos o de uso")
}

/**
 * Clasificación entre bien físico tangible o prestación de servicio técnico/profesional.
 */
enum class ItemType {
    BIEN_FISICO,
    SERVICIO
}

/**
 * Estado del artículo dentro del ciclo de vida del inventario (Figura 7.3 UV).
 */
enum class ItemStatus {
    DISPONIBLE,     // Visible en catálogo y elegible para matching
    RESERVADO,      // Bloqueado atómicamente por una oferta aceptada (ACID lock)
    EN_ENTREGA,     // Cita pactada en Safe Zone con fecha programada
    INTERCAMBIADO,  // Transacción completada mediante confirmación QR
    PAUSADO,        // Retirado temporalmente por el usuario
    ELIMINADO       // Baja lógica del inventario
}

/**
 * Categorías oficiales de alta liquidez según el documento SWEBOK de la UV.
 */
data class CategoryTaxonomy(
    val id: Int,
    val name: String,
    val description: String,
    val isPhysicalGood: Boolean = true,
    val isActive: Boolean = true
) {
    companion object {
        val LIQUID_CATEGORIES = listOf(
            CategoryTaxonomy(1, "Electrónica, Cómputo y Telefonía", "Smartphones, laptops, accesorios, tablets"),
            CategoryTaxonomy(2, "Videojuegos y Entretenimiento", "Consolas, cartuchos, discos, coleccionables"),
            CategoryTaxonomy(3, "Libros Escolares y Universitarios", "Libros de texto, material didáctico, calculadoras"),
            CategoryTaxonomy(4, "Ropa, Calzado y Accesorios", "Prendas de vestir en excelente estado, calzado original"),
            CategoryTaxonomy(5, "Herramientas de Hogar y Taller", "Taladros, juegos de llaves, jardinería"),
            CategoryTaxonomy(6, "Instrumentos Musicales y Audio", "Guitarras, teclados, micrófonos, audífonos"),
            CategoryTaxonomy(7, "Artículos Deportivos y Bicicletas", "Bicicletas, equipo fitness, balones"),
            CategoryTaxonomy(8, "Servicios y Asesorías Técnicas", "Reparación, clases particulares, asesoría técnica", isPhysicalGood = false)
        )
    }
}

/**
 * Banda de valor estimada para equilibrar expectativas (RN-02).
 */
data class EstimatedValueRange(
    val minValue: Double,
    val maxValue: Double
) {
    init {
        require(minValue >= 0.0) { "El valor mínimo no puede ser negativo." }
        require(maxValue >= minValue) { "El valor máximo ($maxValue) debe ser mayor o igual al valor mínimo ($minValue)." }
    }

    val medianValue: Double
        get() = (minValue + maxValue) / 2.0
}

/**
 * Entidad principal que modela un artículo o servicio para trueque.
 */
data class TradeItem(
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String,
    val categoryId: Int,
    val type: ItemType,
    val condition: ItemCondition,
    val estimatedValue: EstimatedValueRange,
    val imagesWebP: List<String>,
    val tagsWishlist: List<String>,
    val status: ItemStatus = ItemStatus.DISPONIBLE,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val isReservable: Boolean
        get() = status == ItemStatus.DISPONIBLE

    val isEditable: Boolean
        get() = status == ItemStatus.DISPONIBLE || status == ItemStatus.PAUSADO

    val estimatedValueMin: Double
        get() = estimatedValue.minValue

    val estimatedValueMax: Double
        get() = estimatedValue.maxValue

    val isAvailable: Boolean
        get() = status == ItemStatus.DISPONIBLE

    /**
     * Constructor secundario simplificado compatible con la especificación canónica de FASE 0.
     */
    constructor(
        id: String,
        ownerId: String,
        title: String,
        description: String,
        categoryId: Int,
        type: ItemType,
        condition: ItemCondition,
        estimatedValueMin: Double,
        estimatedValueMax: Double,
        imagesWebP: List<String> = emptyList(),
        tagsWishlist: List<String> = emptyList(),
        isAvailable: Boolean = true
    ) : this(
        id = id,
        ownerId = ownerId,
        title = title,
        description = description,
        categoryId = categoryId,
        type = type,
        condition = condition,
        estimatedValue = EstimatedValueRange(estimatedValueMin, estimatedValueMax),
        imagesWebP = imagesWebP,
        tagsWishlist = tagsWishlist,
        status = if (isAvailable) ItemStatus.DISPONIBLE else ItemStatus.PAUSADO
    )
}
