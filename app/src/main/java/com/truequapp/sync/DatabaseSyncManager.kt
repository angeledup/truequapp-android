package com.truequapp.sync

import android.util.Log
import com.truequapp.core.database.dao.TruequeDao
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.OfertaItemEntity
import com.truequapp.core.database.entity.TruequeEntity
import com.truequapp.core.database.storage.UserPublicationStorage
import com.truequapp.core.datastore.UserSessionDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object Success : SyncState
    data class ConnectionError(
        val message: String = "Error de conexion"
    ) : SyncState
    data class OtherError(
        val message: String = "Error, intentar mas tarde",
        val terminalError: String
    ) : SyncState
}

@Singleton
class DatabaseSyncManager @Inject constructor(
    private val userDao: UserDao,
    private val truequeDao: TruequeDao,
    private val userSessionDataStore: UserSessionDataStore,
    private val userPublicationStorage: UserPublicationStorage? = null
) {

    var cloudBaseUrl: String = "https://truequapp-pi.vercel.app"

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var syncJob: Job? = null

    /**
     * Inicia el ciclo periódico de actualización de la base de datos local cada 5 segundos.
     * En la primera iteración ejecuta inmediatamente una descarga completa de todo db-Online.
     */
    fun startPeriodicSync(scope: CoroutineScope) {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            while (isActive) {
                syncDatabase()
                delay(5_000L) // 5 segundos exactos
            }
        }
    }

    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
    }

    fun triggerManualSync(scope: CoroutineScope) {
        scope.launch {
            syncDatabase()
        }
    }

    /**
     * Ejecuta una sincronización completa con la base de datos en la nube (Turso).
     * Si falla por conectividad -> SyncState.ConnectionError("Error de conexion")
     * Si falla por otra causa -> SyncState.OtherError("Error, intentar mas tarde", terminalError)
     */
    suspend fun syncDatabase() {
        if (cloudBaseUrl.isBlank()) {
            _syncState.value = SyncState.Success
            return
        }

        _syncState.value = SyncState.Syncing

        try {
            // 1. Sincronizar Catálogo Global
            syncCatalogFromCloud()

            // 2. Sincronizar Propuestas de Trueque
            syncTradesFromCloud()

            // 3. Sincronizar Datos de Usuario y Balance de Monedas
            syncActiveUserFromCloud()

            // 4. Reconciliar propuestas en espera de validación (Outbox: Enviando -> Pendiente)
            reconcilePendingTradeProposals()

            _syncState.value = SyncState.Success
        } catch (t: Throwable) {
            val terminalMessage = formatTerminalError(t)
            try {
                Log.e("DatabaseSync", "Fallo en sincronización periódica (5s):\n$terminalMessage", t)
            } catch (_: Throwable) {
                System.err.println("[DatabaseSync ERROR] $terminalMessage")
            }

            if (isConnectionException(t)) {
                _syncState.value = SyncState.ConnectionError("Error de conexion")
            } else {
                _syncState.value = SyncState.OtherError(
                    message = "Error, intentar mas tarde",
                    terminalError = terminalMessage
                )
            }
        }
    }

    private fun syncCatalogFromCloud() {
        val endpoint = "${cloudBaseUrl.trimEnd('/')}/api/catalogo"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 4500
            conn.readTimeout = 4500
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            if (code == 200) {
                userPublicationStorage?.getRemoteCatalogItems()
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("HTTP $code en GET /api/catalogo: ${conn.responseMessage} $errorBody")
            }
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun syncTradesFromCloud() {
        val endpoint = "${cloudBaseUrl.trimEnd('/')}/api/trueques"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 4500
            conn.readTimeout = 4500
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            if (code == 200) {
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val array = JSONArray(text)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val propId = obj.optString("id")
                    if (propId.isBlank()) continue

                    val uA = obj.optString("initiatorEmail").ifBlank { obj.optString("initiatorUserId", "") }
                    val uB = obj.optString("receiverEmail").ifBlank { obj.optString("receiverUserId", "") }
                    val statusStr = obj.optString("status", "PROPUESTA_ENVIADA")
                    val activa = obj.optBoolean("esta_activa", true)
                    val targetId = obj.optString("targetItemId", "")
                    val offeredArray = obj.optJSONArray("offeredItemIds")
                    val offeredList = mutableListOf<String>()
                    if (offeredArray != null) {
                        for (j in 0 until offeredArray.length()) {
                            offeredList.add(offeredArray.optString(j))
                        }
                    }
                    val coins = obj.optDouble("monetaryCompensationCoins", 0.0)
                    val createdAt = obj.optLong("createdAtEpochMs", System.currentTimeMillis())

                    val ofA = OfertaItemEntity(
                        id = "of_a_$propId",
                        usuarioCorreo = uA,
                        articulosIds = offeredList.joinToString(","),
                        truequeCoins = coins,
                        fechaCreacion = createdAt
                    )
                    val ofB = OfertaItemEntity(
                        id = "of_b_$propId",
                        usuarioCorreo = uB,
                        articulosIds = targetId,
                        truequeCoins = 0.0,
                        fechaCreacion = createdAt
                    )
                    val trueque = TruequeEntity(
                        id = propId,
                        usuarioACorreo = uA,
                        ofertaAId = ofA.id,
                        usuarioBCorreo = uB,
                        ofertaBId = ofB.id,
                        estado = statusStr,
                        estaActiva = activa,
                        fechaCreacion = createdAt,
                        fechaActualizacion = System.currentTimeMillis()
                    )

                    truequeDao.insertOfertaItem(ofA)
                    truequeDao.insertOfertaItem(ofB)
                    truequeDao.insertTrueque(trueque)
                }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("HTTP $code en GET /api/trueques: ${conn.responseMessage} $errorBody")
            }
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun syncActiveUserFromCloud() {
        val session = userSessionDataStore.sessionFlow.first()
        val email = session.userEmail ?: return
        if (email.isBlank()) return

        val endpoint = "${cloudBaseUrl.trimEnd('/')}/api/usuarios/${java.net.URLEncoder.encode(email, "UTF-8")}"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 4500
            conn.readTimeout = 4500
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            if (code == 200) {
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val obj = JSONObject(text)
                val coins = obj.optDouble("truequeCoins", 100.0)
                val trades = obj.optInt("completedTrades", 0)
                val rating = obj.optDouble("bayesianRating", 5.0)

                val localUser = userDao.getUserByEmail(email)
                if (localUser != null) {
                    val updated = localUser.copy(
                        cantidadTruequeCoins = coins,
                        truequesRealizados = trades,
                        calificacion = rating
                    )
                    userDao.updateUser(updated)
                }
                userSessionDataStore.updateTruequeCoins(coins, 0.0)
            } else if (code == 404) {
                // Usuario eliminado en Turso -> purgar localmente
                val localUser = userDao.getUserByEmail(email)
                if (localUser != null) {
                    userDao.deleteUser(localUser.id)
                }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("HTTP $code en GET /api/usuarios: ${conn.responseMessage} $errorBody")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun isConnectionException(t: Throwable): Boolean {
        var curr: Throwable? = t
        while (curr != null) {
            if (curr is UnknownHostException ||
                curr is SocketTimeoutException ||
                curr is ConnectException ||
                curr is NoRouteToHostException ||
                curr is SocketException ||
                (curr is IOException && curr.message?.contains("failed to connect", ignoreCase = true) == true)
            ) {
                return true
            }
            curr = curr.cause
        }
        return false
    }

    private fun formatTerminalError(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        val full = sw.toString()
        val lines = full.lines().filter { it.isNotBlank() }
        val topLines = lines.take(3).joinToString("\n")
        return topLines.ifBlank { "${t.javaClass.simpleName}: ${t.localizedMessage ?: "Unknown error"}" }
    }

    /**
     * Reconcilia propuestas creadas en db-Local que están en espera ('ENVIANDO').
     * Verifica la disponibilidad de la publicación objetivo en db-Online antes de pasarla a 'PENDIENTE'
     * o a 'CONFLICTO_NO_DISPONIBLE'.
     */
    private suspend fun reconcilePendingTradeProposals() {
        val enviandoProposals = try {
            truequeDao.getTruequesEnviando()
        } catch (_: Throwable) {
            emptyList()
        }

        if (enviandoProposals.isEmpty()) return

        for (item in enviandoProposals) {
            val proposal = item.trueque
            val targetItemId = item.ofertaB?.articulosIds.orEmpty()

            val isTargetAvailable = checkTargetItemAvailability(targetItemId)
            if (isTargetAvailable) {
                val registeredInCloud = uploadTradeProposalToCloud(item)
                if (registeredInCloud) {
                    truequeDao.updateEstado(
                        truequeId = proposal.id,
                        nuevoEstado = "PENDIENTE",
                        estaActiva = true
                    )
                }
            } else {
                truequeDao.updateEstado(
                    truequeId = proposal.id,
                    nuevoEstado = "CONFLICTO_NO_DISPONIBLE",
                    estaActiva = false
                )
            }
        }
    }

    private fun checkTargetItemAvailability(itemId: String): Boolean {
        if (itemId.isBlank()) return true
        val endpoint = "${cloudBaseUrl.trimEnd('/')}/api/catalogo"
        return try {
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val array = JSONArray(text)
                var found = false
                var isAvailable = false
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    if (obj.optString("id") == itemId) {
                        found = true
                        val status = obj.optString("status", "DISPONIBLE")
                        isAvailable = status.equals("DISPONIBLE", ignoreCase = true)
                        break
                    }
                }
                conn.disconnect()
                if (found) isAvailable else false
            } else {
                conn.disconnect()
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun uploadTradeProposalToCloud(item: com.truequapp.core.database.entity.TruequeConOfertas): Boolean {
        val endpoint = "${cloudBaseUrl.trimEnd('/')}/api/trueques"
        return try {
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.doOutput = true

            val offeredIds = item.ofertaA?.articulosIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val coins = item.ofertaA?.truequeCoins ?: 0.0

            val json = JSONObject().apply {
                put("id", item.trueque.id)
                put("initiatorUserId", item.trueque.usuarioACorreo)
                put("initiatorEmail", item.trueque.usuarioACorreo)
                put("receiverUserId", item.trueque.usuarioBCorreo)
                put("receiverEmail", item.trueque.usuarioBCorreo)
                put("offeredItemIds", JSONArray(offeredIds))
                put("targetItemId", item.ofertaB?.articulosIds.orEmpty())
                put("monetaryCompensationCoins", coins)
                put("status", "PENDIENTE")
                put("esta_activa", true)
            }

            conn.outputStream.use { os ->
                os.write(json.toString().toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Throwable) {
            false
        }
    }
}

