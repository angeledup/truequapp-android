package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truequapp.core.database.entity.SafeZoneEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceso a puntos seguros de intercambio georreferenciados en Coatzacoalcos.
 */
@Dao
interface SafeZoneDao {

    @Query("SELECT * FROM safe_zones ORDER BY name ASC")
    fun getAllSafeZones(): Flow<List<SafeZoneEntity>>

    @Query("SELECT * FROM safe_zones WHERE id = :id LIMIT 1")
    suspend fun getSafeZoneById(id: String): SafeZoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafeZones(safeZones: List<SafeZoneEntity>)

    @Query("SELECT COUNT(*) FROM safe_zones")
    suspend fun countSafeZones(): Int
}

