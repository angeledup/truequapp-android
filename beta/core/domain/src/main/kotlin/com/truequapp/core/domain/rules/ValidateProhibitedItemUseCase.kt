package com.truequapp.core.domain.rules

/**
 * Caso de uso: Prohibición de Artículos Ilícitos y Políticas de Contenido Google Play (RN-11).
 *
 * Bloquea estrictamente el alta o edición de publicaciones que contengan palabras clave asociadas a:
 * 1. Armas de fuego, municiones, explosivos o armas blancas punzocortantes.
 * 2. Medicamentos de prescripción médica, drogas o sustancias controladas.
 * 3. Especies animales protegidas, fauna silvestre o tráfico biológico.
 * 4. Servicios para adultos, contenido sexual o comercialización de personas.
 * 5. Documentos falsificados, divisas de curso legal o instrumentos financieros no autorizados.
 */
class ValidateProhibitedItemUseCase {

    /**
     * Evalúa si los textos del artículo publicado contienen términos prohibidos por la política de seguridad.
     *
     * @param title Título del artículo o servicio.
     * @param description Descripción detallada.
     * @param tags Lista de etiquetas o términos de búsqueda asignados.
     * @throws BusinessRuleException.ProhibitedItemException si se detecta cualquier coincidencia prohibida.
     */
    operator fun invoke(
        title: String,
        description: String,
        tags: List<String> = emptyList()
    ) {
        val normalizedCombinedText = buildString {
            append(title.lowercase())
            append(" ")
            append(description.lowercase())
            append(" ")
            tags.forEach { append(it.lowercase()).append(" ") }
        }

        for (prohibitedWord in PROHIBITED_KEYWORDS) {
            val regex = Regex("\\b${Regex.escape(prohibitedWord)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(normalizedCombinedText)) {
                throw BusinessRuleException.ProhibitedItemException(matchedKeyword = prohibitedWord)
            }
        }
    }

    companion object {
        /**
         * Diccionario de términos prohibidos estandarizado para la política de seguridad comunitaria.
         */
        val PROHIBITED_KEYWORDS = listOf(
            // Armamento y explosivos
            "pistola", "revolver", "rifle", "escopeta", "municion", "municiones", "bala", "balas",
            "arma de fuego", "explosivo", "dinamita", "granada", "polvora", "navaja automatica",
            // Sustancias y medicamentos
            "droga", "cocaina", "marihuana", "heroina", "metanfetamina", "fentanilo", "lsd",
            "medicamento controlado", "receta medica", "antibiotico", "clonazepam", "psicotropico",
            // Fauna protegida
            "animal exotico", "especie en peligro", "piel de jaguar", "marfil", "fauna silvestre",
            // Contenido adulto y explotación
            "servicio sexual", "acompañante", "escort", "pornografia", "prostitucion",
            // Ilicitud financiera
            "dinero falso", "billete falso", "tarjeta clonada", "hack", "identificacion falsa"
        )
    }
}
