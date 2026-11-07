package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truequapp.core.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la persistencia local reactiva de mensajes de chat (CU018).
 */
@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    /**
     * Observa en tiempo real todos los mensajes asociados a una propuesta de trueque específica.
     */
    @Query("SELECT * FROM chat_mensajes WHERE trueque_id = :truequeId ORDER BY fecha_envio_ms ASC")
    fun getMessagesForTrueque(truequeId: String): Flow<List<ChatMessageEntity>>

    /**
     * Obtiene el último mensaje registrado para una propuesta dada.
     */
    @Query("SELECT * FROM chat_mensajes WHERE trueque_id = :truequeId ORDER BY fecha_envio_ms DESC LIMIT 1")
    suspend fun getLatestMessageForTrueque(truequeId: String): ChatMessageEntity?

    /**
     * Marca como leídos los mensajes recibidos en un trueque.
     */
    @Query("UPDATE chat_mensajes SET esta_leido = 1 WHERE trueque_id = :truequeId AND receptor_correo = :userEmail AND esta_leido = 0")
    suspend fun markMessagesAsRead(truequeId: String, userEmail: String)

    /**
     * Cuenta cuántos mensajes sin leer tiene un usuario en una propuesta dada.
     */
    @Query("SELECT COUNT(*) FROM chat_mensajes WHERE trueque_id = :truequeId AND receptor_correo = :userEmail AND esta_leido = 0")
    fun countUnreadMessages(truequeId: String, userEmail: String): Flow<Int>
}

