package com.truequapp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.truequapp.core.database.dao.ChatMessageDao
import com.truequapp.core.database.dao.LedgerEntryDao
import com.truequapp.core.database.dao.SafeZoneDao
import com.truequapp.core.database.dao.TradeItemDao
import com.truequapp.core.database.dao.TradeProposalDao
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.dao.TruequeDao
import com.truequapp.core.database.entity.ChatMessageEntity
import com.truequapp.core.database.entity.LedgerEntryEntity
import com.truequapp.core.database.entity.OfertaItemEntity
import com.truequapp.core.database.entity.SafeZoneEntity
import com.truequapp.core.database.entity.TradeItemEntity
import com.truequapp.core.database.entity.TradeProposalEntity
import com.truequapp.core.database.entity.TruequeEntity
import com.truequapp.core.database.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base de Datos Room SQLite Principal de Truequapp.
 * Almacena de forma persistente y estructurada:
 * - Usuarios, credenciales (contraseña/Google), proveedor de identidad y balance de TruequeCoins.
 * - Artículos publicados en el catálogo de marketplace.
 * - Propuestas de trueque y citas programadas.
 * - Mensajes de chat y auditoría transaccional (RN-03).
 * - Asientos contables del libro mayor de partida doble (RN-04).
 * - Puntos Seguros de intercambio georreferenciados en Coatzacoalcos (CU019).
 */
@Database(
    entities = [
        UserEntity::class,
        TradeItemEntity::class,
        TradeProposalEntity::class,
        ChatMessageEntity::class,
        LedgerEntryEntity::class,
        SafeZoneEntity::class,
        OfertaItemEntity::class,
        TruequeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TruequappDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun tradeItemDao(): TradeItemDao
    abstract fun tradeProposalDao(): TradeProposalDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun ledgerEntryDao(): LedgerEntryDao
    abstract fun safeZoneDao(): SafeZoneDao
    abstract fun truequeDao(): TruequeDao

    companion object {
        const val DATABASE_NAME = "truequapp_database.db"

        fun buildDatabase(context: Context): TruequappDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TruequappDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(DatabasePrepopulateCallback())
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    private class DatabasePrepopulateCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            seedDatabase(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            seedAdminAccounts(db)
        }

        private fun seedDatabase(db: SupportSQLiteDatabase) {
            // Inserción de Puntos Seguros en Coatzacoalcos
            val insertSafeZoneSql = """
                INSERT OR REPLACE INTO safe_zones (id, name, addressDescription, latitude, longitude, openingHour, closingHour, hasVideoSurveillance, isCampusUniversityPartner, city)
                VALUES 
                ('sz_coatza_forum', 'Punto Seguro Plaza Forum Coatzacoalcos', 'Av. Universidad Veracruzana 2601, Col. El Palmar. Domo central frente a vigilancia 24/7', 18.1415, -94.4852, 10, 21, 1, 0, 'Coatzacoalcos'),
                ('sz_coatza_crystal', 'Punto Seguro Plaza Crystal Coatzacoalcos', 'Av. Universidad y Jacarandas, Col. Rancho Alegre. Módulo de acceso Chedraui', 18.1430, -94.4695, 8, 22, 1, 0, 'Coatzacoalcos'),
                ('sz_coatza_independencia', 'Punto Seguro Parque Independencia - Palacio Municipal', 'Av. Ignacio Zaragoza esq. Carranza, Centro. Explanada con cámaras C4', 18.1486, -94.4208, 7, 22, 1, 0, 'Coatzacoalcos'),
                ('sz_coatza_malecon', 'Punto Seguro Malecón Costero - Hemiciclo', 'Paseo del Malecón Costero esq. Venustiano Carranza. Módulo turístico', 18.1565, -94.4230, 8, 21, 1, 0, 'Coatzacoalcos'),
                ('sz_coatza_uv', 'Punto Seguro Universidad Veracruzana (Campus Coatzacoalcos)', 'Av. Universidad Veracruzana Km 7.5, Col. Santa Isabel. Caseta USBI', 18.1422, -94.4760, 7, 20, 1, 1, 'Coatzacoalcos'),
                ('sz_coatza_acaya', 'Punto Seguro Plaza Acaya Coatzacoalcos', 'Carretera Coatza-Mina Km 9, Col. El Tesoro. Pasillo central', 18.1448, -94.5020, 10, 21, 1, 0, 'Coatzacoalcos'),
                ('sz_coatza_paso_lanchas', 'Punto Seguro Paso de Lanchas Coatzacoalcos', 'Av. Colón esq. Malecón del Río, Centro. Muelle de lanchas', 18.1480, -94.4170, 6, 22, 1, 0, 'Coatzacoalcos');
            """.trimIndent()
            db.execSQL(insertSafeZoneSql)

            seedAdminAccounts(db)
            seedItemsAndProposals(db)
        }

        private fun seedAdminAccounts(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            // Inserción de Cuentas Beta Administradores: chocolatt y Vergara con 10,000 TruequeCoins para pruebas
            val insertAdminsSql = """
                INSERT OR REPLACE INTO users (id, email, name, passwordHash, phoneNumber, authProvider, photoUrl, isKycVerified, kycLevel, truequeCoins, escrowCoins, bayesianRating, ratingsCount, attendanceRate, obfuscatedNeighborhood, city, createdAtEpochMs, lastLoginEpochMs)
                VALUES 
                ('usr_chocolatt', 'chocolattcorreo@gmail.com', 'chocolatt', 'chocolatte', '+529211000001', 'EMAIL', NULL, 1, 'TELEFONO_SMS_NIVEL_1', 10000.0, 0.0, 4.9, 15, 1.0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', $now, $now),
                ('usr_vergara', 'Vergaracorreo@gmail.com', 'Vergara', 'AngEduVr', '+529211000002', 'EMAIL', NULL, 1, 'TELEFONO_SMS_NIVEL_1', 10000.0, 0.0, 4.9, 14, 1.0, 'Col. Petrolera, Coatzacoalcos', 'Coatzacoalcos', $now, $now),
                ('usr_demo_01', 'usuario@truequapp.com', 'Usuario Demo Coatzacoalcos', 'password123', '+529210000000', 'EMAIL', NULL, 1, 'TELEFONO_SMS_NIVEL_1', 10000.0, 0.0, 4.8, 12, 1.0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', $now, $now);
            """.trimIndent()
            db.execSQL(insertAdminsSql)
            // Actualizar saldo a 10,000 TC si ya existían previamente
            db.execSQL("UPDATE users SET truequeCoins = 10000.0 WHERE id IN ('usr_chocolatt', 'usr_vergara', 'usr_demo_01');")
        }

        private fun seedItemsAndProposals(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            // Inserción de Artículos Iniciales vinculados a chocolatt y Vergara
            val insertItemsSql = """
                INSERT OR REPLACE INTO items (id, ownerId, ownerName, title, description, categoryId, condition, type, estimatedValueMin, estimatedValueMax, imagesWebPJson, tagsWishlistJson, status, isBookmarked, locationNeighborhood, city, createdAtEpochMs)
                VALUES 
                ('item_chocolatt_01', 'usr_chocolatt', 'chocolatt', 'Consola Nintendo Switch OLED', 'En caja original con accesorios completos y dos mandos Joy-Con.', 2, 'COMO_NUEVO', 'BIEN_FISICO', 4500.0, 5200.0, '["https://images.unsplash.com/photo-1578303512597-81e6cc155b3e"]', '["Bicicleta", "Laptop Dell", "PlayStation 4"]', 'DISPONIBLE', 0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', $now),
                ('item_chocolatt_02', 'usr_chocolatt', 'chocolatt', 'Audífonos Sony WH-1000XM4 Cancelación', 'Audífonos inalámbricos premium con cancelación activa de ruido y funda de viaje.', 2, 'COMO_NUEVO', 'BIEN_FISICO', 3200.0, 3800.0, '["https://images.unsplash.com/photo-1546435770-a3e426bf472b"]', '["Bicicleta R29", "Gadgets"]', 'DISPONIBLE', 0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', $now),
                ('item_vergara_01', 'usr_vergara', 'Vergara', 'Bicicleta de Montaña R29 Alum Shimano', 'Frenos de disco hidráulicos, 21 velocidades Shimano para rodar en Malecón.', 7, 'USADO', 'BIEN_FISICO', 3200.0, 3800.0, '["https://images.unsplash.com/photo-1485965120184-e220f721d03e"]', '["Nintendo Switch", "Consolas", "Gaming"]', 'DISPONIBLE', 0, 'Col. Petrolera, Coatzacoalcos', 'Coatzacoalcos', $now),
                ('item_vergara_02', 'usr_vergara', 'Vergara', 'Laptop Dell Inspiron 15 Core i5 16GB', 'Intel Core i5 11va Gen, 16GB RAM, SSD 512GB NVMe, pantalla 15.6 FHD.', 2, 'COMO_NUEVO', 'BIEN_FISICO', 6500.0, 7500.0, '["https://images.unsplash.com/photo-1496181133206-80ce9b88a853"]', '["Nintendo Switch OLED", "iPad", "Gaming"]', 'DISPONIBLE', 0, 'Col. Petrolera, Coatzacoalcos', 'Coatzacoalcos', $now);
            """.trimIndent()
            db.execSQL(insertItemsSql)

            // Inserción de Propuesta Inicial entre Vergara y chocolatt
            val insertProposalSql = """
                INSERT OR REPLACE INTO trade_proposals (id, initiatorUserId, receiverUserId, targetItemId, offeredItemIdsJson, monetaryCompensationCoins, status, safeZoneId, safeZoneName, scheduledMeetingEpochMs, qrSecurityToken, createdAtEpochMs, updatedAtEpochMs)
                VALUES 
                ('prop_seed_chocolatt_vergara', 'usr_vergara', 'usr_chocolatt', 'item_chocolatt_01', '["item_vergara_01"]', 500.0, 'PROPUESTA_ENVIADA', 'sz_coatza_forum', 'Punto Seguro Plaza Forum Coatzacoalcos', NULL, 'qr_token_coatza_test_101', $now, $now);
            """.trimIndent()
            db.execSQL(insertProposalSql)

            // Mensajes iniciales en el chat de la propuesta
            val insertChatSql = """
                INSERT OR REPLACE INTO chat_messages (id, proposalId, senderId, senderName, content, timestampEpochMs, isAuditLog, isRead)
                VALUES 
                ('msg_seed_001', 'prop_seed_chocolatt_vergara', 'usr_vergara', 'Vergara', '¡Hola chocolatt! Te ofrezco mi Bicicleta R29 más 500 TruequeCoins por tu Nintendo Switch OLED. ¿Qué opinas?', ${now - 600000}, 0, 1),
                ('msg_seed_002', 'prop_seed_chocolatt_vergara', 'usr_chocolatt', 'chocolatt', '¡Hola Vergara! Me interesa mucho la bicicleta. ¿Nos vemos en Plaza Forum en el domo central?', ${now - 300000}, 0, 1);
            """.trimIndent()
            db.execSQL(insertChatSql)
        }
    }
}

