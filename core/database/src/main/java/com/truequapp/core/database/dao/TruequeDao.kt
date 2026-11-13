package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.truequapp.core.database.entity.OfertaItemEntity
import com.truequapp.core.database.entity.TruequeConOfertas
import com.truequapp.core.database.entity.TruequeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz de Acceso a Datos (DAO) para el motor de trueques y ofertas.
 */
@Dao
interface TruequeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfertaItem(oferta: OfertaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrueque(trueque: TruequeEntity)

    @Update
    suspend fun updateTrueque(trueque: TruequeEntity)

    /**
     * Actualiza el estado y la bandera 'esta_activa' (True si está en proceso, False si se canceló/completó).
     */
    @Query("UPDATE trueques SET estado = :nuevoEstado, esta_activa = :estaActiva, fecha_actualizacion = :timestamp WHERE id = :truequeId")
    suspend fun updateEstado(
        truequeId: String,
        nuevoEstado: String,
        estaActiva: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )

    @Transaction
    @Query("SELECT * FROM trueques WHERE id = :truequeId LIMIT 1")
    suspend fun getTruequeById(truequeId: String): TruequeConOfertas?

    /**
     * Consulta únicamente las propuestas que están marcadas como ACTIVAS (esta_activa = 1).
     */
    @Transaction
    @Query("SELECT * FROM trueques WHERE esta_activa = 1 ORDER BY fecha_actualizacion DESC")
    fun getTruequesActivos(): Flow<List<TruequeConOfertas>>

    /**
     * Consulta las propuestas activas donde participa un usuario (como creador o receptor).
     */
    @Transaction
    @Query("SELECT * FROM trueques WHERE (usuario_a_correo = :correo OR usuario_b_correo = :correo) AND esta_activa = 1 ORDER BY fecha_actualizacion DESC")
    fun getTruequesActivosPorUsuario(correo: String): Flow<List<TruequeConOfertas>>

    /**
     * Historial completo de un usuario (incluye activas e inactivas).
     */
    @Transaction
    @Query("SELECT * FROM trueques WHERE (usuario_a_correo = :correo OR usuario_b_correo = :correo) ORDER BY fecha_actualizacion DESC")
    fun getHistorialTruequesPorUsuario(correo: String): Flow<List<TruequeConOfertas>>

    /**
     * Ofertas recibidas por el usuario B que están activas y pendientes de respuesta.
     */
    @Transaction
    @Query("SELECT * FROM trueques WHERE usuario_b_correo = :correo AND esta_activa = 1 ORDER BY fecha_actualizacion DESC")
    fun getOfertasRecibidas(correo: String): Flow<List<TruequeConOfertas>>

    /**
     * Ofertas enviadas por el usuario A que continúan activas.
     */
    @Transaction
    @Query("SELECT * FROM trueques WHERE usuario_a_correo = :correo AND esta_activa = 1 ORDER BY fecha_actualizacion DESC")
    fun getOfertasEnviadas(correo: String): Flow<List<TruequeConOfertas>>

    @Query("DELETE FROM trueques WHERE id = :truequeId")
    suspend fun deleteTruequeById(truequeId: String)
}

