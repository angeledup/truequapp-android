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
    version = 5,
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
            try {
                seedAdminAccounts(db)
                seedItemsAndProposals(db)
            } catch (e: Throwable) {
                android.util.Log.e("TruequappDatabase", "Error en onOpen al sembrar datos", e)
            }
        }

        private fun seedDatabase(db: SupportSQLiteDatabase) {
            try {
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
            } catch (e: Throwable) {
                android.util.Log.e("TruequappDatabase", "Error al sembrar safe zones", e)
            }

            seedAdminAccounts(db)
            seedItemsAndProposals(db)
        }

        private fun seedAdminAccounts(db: SupportSQLiteDatabase) {
            try {
                val now = System.currentTimeMillis()
                // Inserción de Cuentas Beta Administradores: chocolatt y Vergara con 10,000 TruequeCoins para pruebas
                val insertAdminsSql = """
                    INSERT OR REPLACE INTO users (id, email, name, passwordHash, phoneNumber, authProvider, photoUrl, isKycVerified, kycLevel, truequeCoins, escrowCoins, bayesianRating, ratingsCount, attendanceRate, obfuscatedNeighborhood, city, completedTrades, createdAtEpochMs, lastLoginEpochMs)
                    VALUES 
                    ('usr_chocolatt', 'chocolattcorreo@gmail.com', 'chocolatt', 'chocolatte', '+529211000001', 'EMAIL', NULL, 1, 'TELEFONO_SMS_NIVEL_1', 10000.0, 0.0, 4.9, 15, 1.0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', 0, $now, $now),
                    ('usr_vergara', 'Vergaracorreo@gmail.com', 'Vergara', 'AngEduVr', '+529211000002', 'EMAIL', NULL, 1, 'TELEFONO_SMS_NIVEL_1', 10000.0, 0.0, 4.9, 14, 1.0, 'Col. Petrolera, Coatzacoalcos', 'Coatzacoalcos', 0, $now, $now),
                    ('usr_demo_01', 'usuario@truequapp.com', 'Usuario Demo Coatzacoalcos', 'password123', '+529210000000', 'EMAIL', NULL, 1, 'TELEFONO_SMS_NIVEL_1', 10000.0, 0.0, 4.8, 12, 1.0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', 0, $now, $now);
                """.trimIndent()
                db.execSQL(insertAdminsSql)
                // Actualizar saldo a 10,000 TC si ya existían previamente
                db.execSQL("UPDATE users SET truequeCoins = 10000.0 WHERE id IN ('usr_chocolatt', 'usr_vergara', 'usr_demo_01');")
            } catch (e: Throwable) {
                android.util.Log.e("TruequappDatabase", "Error al sembrar administradores", e)
            }
        }

        private fun seedItemsAndProposals(db: SupportSQLiteDatabase) {
            try {
                val now = System.currentTimeMillis()
                // 1. Inserción de Artículos Iniciales vinculados a chocolatt y Vergara
                val insertItemsSql = """
                    INSERT OR REPLACE INTO items (id, ownerId, ownerName, title, description, categoryId, condition, type, estimatedValueMin, estimatedValueMax, imagesWebPJson, tagsWishlistJson, status, isBookmarked, locationNeighborhood, city, createdAtEpochMs)
                    VALUES 
                    ('item_chocolatt_01', 'usr_chocolatt', 'chocolatt', 'Consola Nintendo Switch OLED', 'En caja original con accesorios completos y dos mandos Joy-Con.', 2, 'COMO_NUEVO', 'BIEN_FISICO', 4500.0, 5200.0, '["https://images.unsplash.com/photo-1578303512597-81e6cc155b3e"]', '["Bicicleta", "Laptop Dell", "PlayStation 4"]', 'DISPONIBLE', 0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', $now),
                    ('item_chocolatt_02', 'usr_chocolatt', 'chocolatt', 'Audífonos Sony WH-1000XM4 Cancelación', 'Audífonos inalámbricos premium con cancelación activa de ruido y funda de viaje.', 2, 'COMO_NUEVO', 'BIEN_FISICO', 3200.0, 3800.0, '["https://images.unsplash.com/photo-1546435770-a3e426bf472b"]', '["Bicicleta R29", "Gadgets"]', 'DISPONIBLE', 0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', $now),
                    ('item_vergara_01', 'usr_vergara', 'Vergara', 'Bicicleta de Montaña R29 Alum Shimano', 'Frenos de disco hidráulicos, 21 velocidades Shimano para rodar en Malecón.', 7, 'USADO', 'BIEN_FISICO', 3200.0, 3800.0, '["https://images.unsplash.com/photo-1485965120184-e220f721d03e"]', '["Nintendo Switch", "Consolas", "Gaming"]', 'DISPONIBLE', 0, 'Col. Petrolera, Coatzacoalcos', 'Coatzacoalcos', $now),
                    ('item_vergara_02', 'usr_vergara', 'Vergara', 'Laptop Dell Inspiron 15 Core i5 16GB', 'Intel Core i5 11va Gen, 16GB RAM, SSD 512GB NVMe, pantalla 15.6 FHD.', 2, 'COMO_NUEVO', 'BIEN_FISICO', 6500.0, 7500.0, '["https://images.unsplash.com/photo-1496181133206-80ce9b88a853"]', '["Nintendo Switch OLED", "iPad", "Gaming"]', 'DISPONIBLE', 0, 'Col. Petrolera, Coatzacoalcos', 'Coatzacoalcos', $now);
                """.trimIndent()
                db.execSQL(insertItemsSql)

                // 2. Inserción de Paquetes de Oferta (tabla ofertas_items requerida por TruequeDao)
                val insertOfertasSql = """
                    INSERT OR REPLACE INTO ofertas_items (id, usuario_correo, articulos_ids, trueque_coins, fecha_creacion)
                    VALUES 
                    ('of_v2c_01', 'Vergaracorreo@gmail.com', 'item_vergara_01', 500.0, $now),
                    ('of_v2c_01_target', 'chocolattcorreo@gmail.com', 'item_chocolatt_01', 0.0, $now),
                    ('of_v2c_02', 'Vergaracorreo@gmail.com', 'item_vergara_02', 0.0, $now),
                    ('of_v2c_02_target', 'chocolattcorreo@gmail.com', 'item_chocolatt_02', 0.0, $now),
                    ('of_c2v_01', 'chocolattcorreo@gmail.com', 'item_chocolatt_01', 1200.0, $now),
                    ('of_c2v_01_target', 'Vergaracorreo@gmail.com', 'item_vergara_02', 0.0, $now),
                    ('of_c2v_02', 'chocolattcorreo@gmail.com', 'item_chocolatt_02', 300.0, $now),
                    ('of_c2v_02_target', 'Vergaracorreo@gmail.com', 'item_vergara_01', 0.0, $now);
                """.trimIndent()
                db.execSQL(insertOfertasSql)

                // 3. Inserción de Contratos de Trueque (tabla trueques con esta_activa = 1, requerida por TradeRepository)
                // 2 propuestas originadas por Vergara hacia chocolatt, y 2 propuestas originadas por chocolatt hacia Vergara
                val insertTruequesSql = """
                    INSERT OR REPLACE INTO trueques (id, usuario_a_correo, oferta_a_id, usuario_b_correo, oferta_b_id, estado, esta_activa, fecha_creacion, fecha_actualizacion)
                    VALUES 
                    ('prop_seed_v2c_01', 'Vergaracorreo@gmail.com', 'of_v2c_01', 'chocolattcorreo@gmail.com', 'of_v2c_01_target', 'PENDIENTE', 1, $now, $now),
                    ('prop_seed_v2c_02', 'Vergaracorreo@gmail.com', 'of_v2c_02', 'chocolattcorreo@gmail.com', 'of_v2c_02_target', 'PENDIENTE', 1, $now, $now),
                    ('prop_seed_c2v_01', 'chocolattcorreo@gmail.com', 'of_c2v_01', 'Vergaracorreo@gmail.com', 'of_c2v_01_target', 'PENDIENTE', 1, $now, $now),
                    ('prop_seed_c2v_02', 'chocolattcorreo@gmail.com', 'of_c2v_02', 'Vergaracorreo@gmail.com', 'of_c2v_02_target', 'PENDIENTE', 1, $now, $now);
                """.trimIndent()
                db.execSQL(insertTruequesSql)

                // 4. Inserción en tabla trade_proposals (para TradeProposalDao)
                val insertProposalSql = """
                    INSERT OR REPLACE INTO trade_proposals (id, initiatorUserId, receiverUserId, targetItemId, offeredItemIdsJson, monetaryCompensationCoins, status, safeZoneId, safeZoneName, scheduledMeetingEpochMs, qrSecurityToken, createdAtEpochMs, updatedAtEpochMs)
                    VALUES 
                    ('prop_seed_v2c_01', 'usr_vergara', 'usr_chocolatt', 'item_chocolatt_01', '["item_vergara_01"]', 500.0, 'PROPUESTA_ENVIADA', 'sz_coatza_forum', 'Punto Seguro Plaza Forum Coatzacoalcos', NULL, 'qr_token_v2c_01', $now, $now),
                    ('prop_seed_v2c_02', 'usr_vergara', 'usr_chocolatt', 'item_chocolatt_02', '["item_vergara_02"]', 0.0, 'PROPUESTA_ENVIADA', 'sz_coatza_crystal', 'Punto Seguro Plaza Crystal Coatzacoalcos', NULL, 'qr_token_v2c_02', $now, $now),
                    ('prop_seed_c2v_01', 'usr_chocolatt', 'usr_vergara', 'item_vergara_02', '["item_chocolatt_01"]', 1200.0, 'PROPUESTA_ENVIADA', 'sz_coatza_independencia', 'Punto Seguro Parque Independencia', NULL, 'qr_token_c2v_01', $now, $now),
                    ('prop_seed_c2v_02', 'usr_chocolatt', 'usr_vergara', 'item_vergara_01', '["item_chocolatt_02"]', 300.0, 'PROPUESTA_ENVIADA', 'sz_coatza_malecon', 'Punto Seguro Malecón Costero', NULL, 'qr_token_c2v_02', $now, $now);
                """.trimIndent()
                db.execSQL(insertProposalSql)

                // 5. Mensajes en chat para cada una de las 4 propuestas (tabla chat_mensajes)
                val insertChatSql = """
                    INSERT OR REPLACE INTO chat_mensajes (id, trueque_id, emisor_correo, receptor_correo, contenido, fecha_envio_ms, esta_entregado, esta_leido)
                    VALUES 
                    ('msg_v2c_01_1', 'prop_seed_v2c_01', 'Vergaracorreo@gmail.com', 'chocolattcorreo@gmail.com', '¡Hola chocolatt! Te ofrezco mi Bicicleta R29 más 500 TruequeCoins por tu Nintendo Switch OLED.', ${now - 600000}, 1, 1),
                    ('msg_v2c_01_2', 'prop_seed_v2c_01', 'chocolattcorreo@gmail.com', 'Vergaracorreo@gmail.com', '¡Hola Vergara! Me interesa mucho la bicicleta. ¿Nos vemos en Plaza Forum en el domo central?', ${now - 300000}, 1, 1),
                    ('msg_v2c_02_1', 'prop_seed_v2c_02', 'Vergaracorreo@gmail.com', 'chocolattcorreo@gmail.com', 'Hola chocolatt, ¿te interesa cambiar tus audífonos Sony WH-1000XM4 por mi Laptop Dell i5 de 16GB?', ${now - 500000}, 1, 1),
                    ('msg_c2v_01_1', 'prop_seed_c2v_01', 'chocolattcorreo@gmail.com', 'Vergaracorreo@gmail.com', '¡Qué tal Vergara! Me gusta tu Laptop Dell. Te ofrezco mi Nintendo Switch OLED más 1200 TC de diferencia.', ${now - 400000}, 1, 1),
                    ('msg_c2v_02_1', 'prop_seed_c2v_02', 'chocolattcorreo@gmail.com', 'Vergaracorreo@gmail.com', 'Hola Vergara, te propongo mis audífonos Sony más 300 TruequeCoins por tu Bicicleta R29.', ${now - 200000}, 1, 1);
                """.trimIndent()
                db.execSQL(insertChatSql)
            } catch (e: Throwable) {
                android.util.Log.e("TruequappDatabase", "Error al sembrar items y propuestas", e)
            }
        }
    }
}

