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
    version = 6,
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
                // Limpiar artículos, propuestas y chats de prueba/semilla
                db.execSQL("DELETE FROM items WHERE id LIKE 'item_chocolatt_%' OR id LIKE 'item_vergara_%' OR id LIKE 'item_00%' OR id LIKE 'item_user_%';")
                db.execSQL("DELETE FROM ofertas_items WHERE id LIKE 'of_v2c_%' OR id LIKE 'of_c2v_%';")
                db.execSQL("DELETE FROM trueques WHERE id LIKE 'prop_seed_%';")
                db.execSQL("DELETE FROM trade_proposals WHERE id LIKE 'prop_seed_%';")
                db.execSQL("DELETE FROM chat_mensajes WHERE id LIKE 'msg_v2c_%' OR id LIKE 'msg_c2v_%' OR trueque_id LIKE 'prop_seed_%';")
            } catch (e: Throwable) {
                android.util.Log.e("TruequappDatabase", "Error en onOpen al actualizar datos", e)
            }
        }

        private fun seedDatabase(db: SupportSQLiteDatabase) {
            try {
                // Inserción de Puntos Seguros en Coatzacoalcos (incluyendo CETIS 79)
                val insertSafeZoneSql = """
                    INSERT OR REPLACE INTO safe_zones (id, name, addressDescription, latitude, longitude, openingHour, closingHour, hasVideoSurveillance, isCampusUniversityPartner, city)
                    VALUES 
                    ('sz_coatza_cetis79', 'Punto Seguro CETIS 79 Coatzacoalcos', 'Av. Universidad Veracruzana Km 8, Col. Las Gaviotas / El Tesoro. Caseta de vigilancia acceso escolar y domo cívico', 18.1435, -94.4930, 7, 20, 1, 1, 'Coatzacoalcos'),
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
        }

        private fun seedAdminAccounts(db: SupportSQLiteDatabase) {
            try {
                val now = System.currentTimeMillis()
                // Inserción de Cuentas Administradores: chocolatt, Vergara con 10,000 TruequeCoins, demo y angeleduvr
                val insertAdminsSql = """
                    INSERT OR REPLACE INTO usuarios (correo, contrasena, usuario, numero_telefonico, cantidad_trueque_coins, trueques_realizados, calificacion)
                    VALUES 
                    ('chocolattcorreo@gmail.com', 'chocolatte', 'chocolatt', '+529211000001', 10000.0, 10, 5.0),
                    ('Vergaracorreo@gmail.com', 'AngEduVr', 'Vergara', '+529211000002', 10000.0, 10, 5.0),
                    ('usuario@truequapp.com', 'password123', 'usuario_demo', '+529210000000', 10000.0, 5, 4.8),
                    ('angeleduvr@gmail.com', '123456', 'Admin_AEVR', '+529212404565', 100000000.0, 10, 10.0);
                """.trimIndent()
                db.execSQL(insertAdminsSql)
                // Asegurar saldo de 10,000 TC
                db.execSQL("UPDATE usuarios SET cantidad_trueque_coins = 10000.0 WHERE correo IN ('chocolattcorreo@gmail.com', 'Vergaracorreo@gmail.com', 'usuario@truequapp.com');")
            } catch (e: Throwable) {
                android.util.Log.e("TruequappDatabase", "Error al sembrar administradores", e)
            }
        }
    }
}

