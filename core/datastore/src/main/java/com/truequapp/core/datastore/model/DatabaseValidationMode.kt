package com.truequapp.core.datastore.model

/**
 * Modos oficiales de validación de base de datos en Truequapp:
 * - [DB_LOCAL]: Validaciones resueltas exclusivamente en la base de datos interna SQLite / Room del dispositivo.
 * - [DB_ONLINE]: Validaciones resueltas contra la base de datos central en Turso Cloud vía API REST.
 */
enum class DatabaseValidationMode {
    DB_LOCAL,
    DB_ONLINE
}
