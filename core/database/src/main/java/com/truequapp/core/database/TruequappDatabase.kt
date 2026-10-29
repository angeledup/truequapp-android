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
import com.truequapp.core.database.entity.ChatMessageEntity
import com.truequapp.core.database.entity.LedgerEntryEntity
import com.truequapp.core.database.entity.SafeZoneEntity
import com.truequapp.core.database.entity.TradeItemEntity
import com.truequapp.core.database.entity.TradeProposalEntity
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
        SafeZoneEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TruequappDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun tradeItemDao(): TradeItemDao
    abstract fun tradeProposalDao(): TradeProposalDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun ledgerEntryDao(): LedgerEntryDao
    abstract fun safeZoneDao(): SafeZoneDao

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
            CoroutineScope(Dispatchers.IO).launch {
                // Inserción de Puntos Seguros en Coatzacoalcos
                val insertSafeZoneSql = """
                    INSERT INTO safe_zones (id, name, addressDescription, latitude, longitude, openingHour, closingHour, hasVideoSurveillance, isCampusUniversityPartner, city)
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

                // Inserción de Usuario Demo Canónico con 100 TruequeCoins
                val insertDemoUserSql = """
                    INSERT INTO users (id, email, name, passwordHash, phoneNumber, authProvider, photoUrl, isKycVerified, kycLevel, truequeCoins, escrowCoins, bayesianRating, ratingsCount, attendanceRate, obfuscatedNeighborhood, city, createdAtEpochMs, lastLoginEpochMs)
                    VALUES ('usr_demo_01', 'usuario@truequapp.com', 'Usuario Demo Coatzacoalcos', 'password123', '+529210000000', 'EMAIL', NULL, 1, 'TELEFONO_SMS_NIVEL_1', 100.0, 0.0, 4.8, 12, 1.0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', ${System.currentTimeMillis()}, ${System.currentTimeMillis()});
                """.trimIndent()
                db.execSQL(insertDemoUserSql)

                // Inserción de Artículos Iniciales en Coatzacoalcos
                val insertItemsSql = """
                    INSERT INTO items (id, ownerId, ownerName, title, description, categoryId, condition, type, estimatedValueMin, estimatedValueMax, imagesWebPJson, tagsWishlistJson, status, isBookmarked, locationNeighborhood, city, createdAtEpochMs)
                    VALUES 
                    ('item_coatza_01', 'usr_demo_01', 'Usuario Demo Coatzacoalcos', 'Consola Nintendo Switch OLED', 'En caja original con accesorios completos y dos mandos Joy-Con.', 2, 'COMO_NUEVO', 'BIEN_FISICO', 4500.0, 5200.0, '["https://images.unsplash.com/photo-1578303512597-81e6cc155b3e"]', '["PlayStation 4", "Tablet Samsung"]', 'DISPONIBLE', 0, 'Centro, Coatzacoalcos', 'Coatzacoalcos', ${System.currentTimeMillis()}),
                    ('item_coatza_02', 'usr_alpha', 'Carlos R. - Petrolera', 'Cálculo de Stewart 8va Edición', 'Libro universitario para ingeniería UV, pasta dura sin rayones.', 3, 'USADO', 'BIEN_FISICO', 500.0, 700.0, '["https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c"]', '["Álgebra Lineal", "Física Universitaria"]', 'DISPONIBLE', 0, 'Col. Petrolera, Coatzacoalcos', 'Coatzacoalcos', ${System.currentTimeMillis()}),
                    ('item_coatza_03', 'usr_beta', 'Lucía M. - María de la Piedad', 'Bicicleta de Montaña R29 Alum', 'Frenos de disco hidráulicos, 21 velocidades Shimano para rodar en Malecón.', 7, 'USADO', 'BIEN_FISICO', 3200.0, 3800.0, '["https://images.unsplash.com/photo-1485965120184-e220f721d03e"]', '["Laptop", "Instrumentos"]', 'DISPONIBLE', 0, 'Col. María de la Piedad, Coatzacoalcos', 'Coatzacoalcos', ${System.currentTimeMillis()});
                """.trimIndent()
                db.execSQL(insertItemsSql)
            }
        }
    }
}
