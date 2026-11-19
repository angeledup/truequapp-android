package com.truequapp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.truequapp.core.datastore.model.DatabaseValidationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor del modo de validación de base de datos (db-Local vs db-Online).
 * Provee reactividad con [validationMode] y funciones directas para conmutar entre ambos modos.
 */
interface DatabaseValidationModeManager {
    val validationMode: StateFlow<DatabaseValidationMode>
    fun getValidationMode(): DatabaseValidationMode
    suspend fun enableDbOnlineValidation()
    suspend fun enableDbLocalValidation()
    suspend fun setValidationMode(mode: DatabaseValidationMode)
}

@Singleton
class DatabaseValidationModeManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : DatabaseValidationModeManager {

    companion object {
        private val KEY_VALIDATION_MODE = stringPreferencesKey("database_validation_mode")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _validationMode = MutableStateFlow(DatabaseValidationMode.DB_ONLINE)
    override val validationMode: StateFlow<DatabaseValidationMode> = _validationMode.asStateFlow()

    init {
        scope.launch {
            dataStore.data.map { prefs ->
                val modeStr = prefs[KEY_VALIDATION_MODE]
                if (modeStr == DatabaseValidationMode.DB_LOCAL.name) {
                    DatabaseValidationMode.DB_LOCAL
                } else {
                    DatabaseValidationMode.DB_ONLINE
                }
            }.collect { mode ->
                _validationMode.value = mode
            }
        }
    }

    override fun getValidationMode(): DatabaseValidationMode = _validationMode.value

    override suspend fun enableDbOnlineValidation() {
        setValidationMode(DatabaseValidationMode.DB_ONLINE)
    }

    override suspend fun enableDbLocalValidation() {
        setValidationMode(DatabaseValidationMode.DB_LOCAL)
    }

    override suspend fun setValidationMode(mode: DatabaseValidationMode) {
        _validationMode.value = mode
        dataStore.edit { prefs ->
            prefs[KEY_VALIDATION_MODE] = mode.name
        }
    }
}
