package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.truequapp.core.database.entity.TradeProposalEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestión de propuestas de trueque.
 */
@Dao
interface TradeProposalDao {

    @Query("SELECT * FROM trade_proposals ORDER BY updatedAtEpochMs DESC")
    fun getAllProposals(): Flow<List<TradeProposalEntity>>

    @Query("SELECT * FROM trade_proposals ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllProposalsSync(): List<TradeProposalEntity>

    @Query("SELECT * FROM trade_proposals WHERE id = :id LIMIT 1")
    fun getProposalById(id: String): Flow<TradeProposalEntity?>

    @Query("SELECT * FROM trade_proposals WHERE id = :id LIMIT 1")
    suspend fun getProposalByIdSync(id: String): TradeProposalEntity?

    @Query("SELECT * FROM trade_proposals WHERE initiatorUserId = :userId OR receiverUserId = :userId ORDER BY updatedAtEpochMs DESC")
    fun getProposalsForUser(userId: String): Flow<List<TradeProposalEntity>>

    @Query("SELECT * FROM trade_proposals WHERE initiatorUserId = :userId OR receiverUserId = :userId ORDER BY updatedAtEpochMs DESC")
    suspend fun getProposalsForUserSync(userId: String): List<TradeProposalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposal(proposal: TradeProposalEntity)

    @Update
    suspend fun updateProposal(proposal: TradeProposalEntity)

    @Query("UPDATE trade_proposals SET status = :status, updatedAtEpochMs = :timestamp WHERE id = :proposalId")
    suspend fun updateStatus(proposalId: String, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM trade_proposals WHERE id = :proposalId")
    suspend fun deleteProposal(proposalId: String)
}

