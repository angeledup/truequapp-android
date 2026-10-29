package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.truequapp.core.database.entity.TradeItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceso y persistencia de artículos del catálogo en SQLite.
 */
@Dao
interface TradeItemDao {

    @Query("SELECT * FROM items WHERE status = 'DISPONIBLE' ORDER BY createdAtEpochMs DESC")
    fun getAllAvailableItems(): Flow<List<TradeItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<TradeItemEntity?>

    @Query("SELECT * FROM items WHERE ownerId = :ownerId ORDER BY createdAtEpochMs DESC")
    fun getItemsByOwner(ownerId: String): Flow<List<TradeItemEntity>>

    @Query("SELECT * FROM items WHERE isBookmarked = 1 AND status = 'DISPONIBLE'")
    fun getBookmarkedItems(): Flow<List<TradeItemEntity>>

    @Query("UPDATE items SET isBookmarked = :isBookmarked WHERE id = :itemId")
    suspend fun setBookmarkStatus(itemId: String, isBookmarked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TradeItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TradeItemEntity>)

    @Update
    suspend fun updateItem(item: TradeItemEntity)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("SELECT COUNT(*) FROM items")
    suspend fun countItems(): Int
}
