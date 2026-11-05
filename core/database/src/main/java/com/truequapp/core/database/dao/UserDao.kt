package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truequapp.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz de Acceso a Datos (DAO) con consultas SQL explícitas para la tabla 'usuarios'.
 */
@Dao
interface UserDao {

    /**
     * Consulta SQL para obtener un usuario a partir de su correo electrónico.
     * Utilizada durante el proceso de Login.
     */
    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    suspend fun getUserByEmail(correo: String): UserEntity?

    /**
     * Consulta SQL reactiva para observar cambios en un usuario en tiempo real.
     */
    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    fun observeUserByEmail(correo: String): Flow<UserEntity?>

    /**
     * Consulta SQL para verificar la existencia previa de un correo en el registro.
     */
    @Query("SELECT COUNT(*) FROM usuarios WHERE LOWER(correo) = LOWER(:correo)")
    suspend fun countByEmail(correo: String): Int

    /**
     * Sentencia SQL INSERT para persistir una nueva cuenta con sus 7 atributos.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    /**
     * Consulta SQL para listar la totalidad de cuentas registradas en la base de datos local.
     */
    @Query("SELECT * FROM usuarios")
    fun getAllUsers(): Flow<List<UserEntity>>
}
