package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truequapp.core.database.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestión del libro contable inmutable de partida doble (RN-04).
 */
@Dao
interface LedgerEntryDao {

    @Query("SELECT * FROM ledger_entries WHERE walletId = :walletId ORDER BY timestampEpochMs DESC")
    fun getEntriesForWallet(walletId: String): Flow<List<LedgerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LedgerEntryEntity>)

    @Query("SELECT * FROM ledger_entries WHERE tradeId = :tradeId ORDER BY timestampEpochMs ASC")
    fun getEntriesForTrade(tradeId: String): Flow<List<LedgerEntryEntity>>
}
