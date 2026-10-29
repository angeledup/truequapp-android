package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truequapp.core.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestión del historial del chat y auditoría transaccional (RN-03).
 */
@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE proposalId = :proposalId ORDER BY timestampEpochMs ASC")
    fun getMessagesForProposal(proposalId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE proposalId = :proposalId AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(proposalId: String, currentUserId: String)
}
