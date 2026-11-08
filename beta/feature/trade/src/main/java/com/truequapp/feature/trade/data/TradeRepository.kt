package com.truequapp.feature.trade.data

import com.truequapp.core.database.dao.TruequeDao
import com.truequapp.core.database.entity.OfertaItemEntity
import com.truequapp.core.database.entity.TruequeConOfertas
import com.truequapp.core.database.entity.TruequeEntity
import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio del Módulo 3 para la persistencia del Motor de Propuestas de Trueque.
 * Implementa el flujo del boceto de arquitectura:
 * - Usuario A almacena localmente su Oferta A (artículos propios + monedas compensatorias).
 * - Usuario B almacena localmente su Oferta B (artículos solicitados / contraoferta).
 * - Registro central 'Trueque' consolida la negociación unificando a ambos usuarios,
 *   referenciando las dos ofertas y manteniendo la variable booleana 'esta_activa'.
 */
@Singleton
class TradeRepository @Inject constructor(
    private val truequeDao: TruequeDao
) {

    /**
     * Registra una nueva propuesta de trueque guardando Oferta A, Oferta B y el Trueque central.
     */
    suspend fun saveProposal(
        proposal: TradeProposal,
        userACorreo: String,
        userBCorreo: String
    ): TruequeConOfertas {
        val ofertaA = OfertaItemEntity(
            id = "of_a_${proposal.id}",
            usuarioCorreo = userACorreo,
            articulosIds = proposal.offeredItemIds.joinToString(","),
            truequeCoins = proposal.compensatoryTruequeCoins,
            fechaCreacion = proposal.createdAtEpochMs
        )
        val ofertaB = OfertaItemEntity(
            id = "of_b_${proposal.id}",
            usuarioCorreo = userBCorreo,
            articulosIds = proposal.targetItemId,
            truequeCoins = 0.0,
            fechaCreacion = proposal.createdAtEpochMs
        )
        val trueque = TruequeEntity(
            id = proposal.id,
            usuarioACorreo = userACorreo,
            ofertaAId = ofertaA.id,
            usuarioBCorreo = userBCorreo,
            ofertaBId = ofertaB.id,
            estado = proposal.status.name,
            estaActiva = proposal.estaActiva,
            fechaCreacion = proposal.createdAtEpochMs,
            fechaActualizacion = System.currentTimeMillis()
        )

        truequeDao.insertOfertaItem(ofertaA)
        truequeDao.insertOfertaItem(ofertaB)
        truequeDao.insertTrueque(trueque)

        // Sincronizar en la nube con Vercel y Turso
        val cloudUrl = "https://truequapp-pi.vercel.app/api/trueques"
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            try {
                val conn = java.net.URL(cloudUrl).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                val json = org.json.JSONObject().apply {
                    put("id", proposal.id)
                    put("initiatorUserId", userACorreo)
                    put("initiatorEmail", userACorreo)
                    put("receiverUserId", userBCorreo)
                    put("receiverEmail", userBCorreo)
                    put("offeredItemIds", org.json.JSONArray(proposal.offeredItemIds))
                    put("targetItemId", proposal.targetItemId)
                    put("monetaryCompensationCoins", proposal.compensatoryTruequeCoins)
                    put("status", proposal.status.name)
                    put("esta_activa", proposal.estaActiva)
                }
                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return TruequeConOfertas(
            trueque = trueque,
            ofertaA = ofertaA,
            ofertaB = ofertaB
        )
    }

    /**
     * Actualiza el estado del trueque y calcula la variable booleana 'esta_activa' (True si está activa, False si no).
     */
    suspend fun updateProposalStatus(proposalId: String, newStatus: TradeStatus) {
        val estaActiva = newStatus in listOf(
            TradeStatus.PENDIENTE,
            TradeStatus.CONTRAOFERTADA,
            TradeStatus.RESERVADO,
            TradeStatus.EN_ENTREGA
        )
        truequeDao.updateEstado(
            truequeId = proposalId,
            nuevoEstado = newStatus.name,
            estaActiva = estaActiva
        )

        // Sincronizar actualización de estado en la nube
        val cloudUrl = "https://truequapp-pi.vercel.app/api/trueques/$proposalId/status"
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            try {
                val conn = java.net.URL(cloudUrl).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "PUT"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                val json = org.json.JSONObject().apply {
                    put("status", newStatus.name)
                }
                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncProposalsFromCloud(userEmail: String) {
        if (userEmail.isBlank()) return
        val cloudUrl = "https://truequapp-pi.vercel.app/api/trueques?user=${java.net.URLEncoder.encode(userEmail, "UTF-8")}"
        try {
            val url = java.net.URL(cloudUrl)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()
                val jsonArray = org.json.JSONArray(text)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val propId = obj.optString("id")
                    if (propId.isBlank()) continue
                    val uA = obj.optString("initiatorUserId", obj.optString("initiatorEmail", ""))
                    val uB = obj.optString("receiverUserId", obj.optString("receiverEmail", ""))
                    val statusStr = obj.optString("status", "PENDIENTE")
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

                    val ofA = OfertaItemEntity(
                        id = "of_a_$propId",
                        usuarioCorreo = uA,
                        articulosIds = offeredList.joinToString(","),
                        truequeCoins = coins,
                        fechaCreacion = System.currentTimeMillis()
                    )
                    val ofB = OfertaItemEntity(
                        id = "of_b_$propId",
                        usuarioCorreo = uB,
                        articulosIds = targetId,
                        truequeCoins = 0.0,
                        fechaCreacion = System.currentTimeMillis()
                    )
                    val tr = TruequeEntity(
                        id = propId,
                        usuarioACorreo = uA,
                        ofertaAId = ofA.id,
                        usuarioBCorreo = uB,
                        ofertaBId = ofB.id,
                        estado = statusStr,
                        estaActiva = activa,
                        fechaCreacion = System.currentTimeMillis(),
                        fechaActualizacion = System.currentTimeMillis()
                    )
                    truequeDao.insertOfertaItem(ofA)
                    truequeDao.insertOfertaItem(ofB)
                    truequeDao.insertTrueque(tr)
                }
            } else {
                conn.disconnect()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Aplica una contraoferta (CU011) actualizando la oferta de ítems/créditos
     * y colocando el estado en CONTRAOFERTADA con esta_activa = true.
     */
    suspend fun submitCounterOffer(
        proposalId: String,
        newOfferedItemIds: List<String>,
        newTruequeCoins: Double
    ) {
        val existing = truequeDao.getTruequeById(proposalId)
        val ofertaA = existing?.ofertaA
        if (ofertaA != null) {
            val updatedOfertaA = ofertaA.copy(
                articulosIds = newOfferedItemIds.joinToString(","),
                truequeCoins = newTruequeCoins
            )
            truequeDao.insertOfertaItem(updatedOfertaA)
        }
        updateProposalStatus(proposalId, TradeStatus.CONTRAOFERTADA)
    }

    suspend fun getProposalById(proposalId: String): TruequeConOfertas? {
        return truequeDao.getTruequeById(proposalId)
    }

    fun getActiveProposalsForUser(correo: String): Flow<List<TruequeConOfertas>> {
        return truequeDao.getTruequesActivosPorUsuario(correo)
    }

    fun getReceivedOffersForUser(correo: String): Flow<List<TruequeConOfertas>> {
        return truequeDao.getOfertasRecibidas(correo)
    }

    fun getSentOffersForUser(correo: String): Flow<List<TruequeConOfertas>> {
        return truequeDao.getOfertasEnviadas(correo)
    }

    fun getHistoryForUser(correo: String): Flow<List<TruequeConOfertas>> {
        return truequeDao.getHistorialTruequesPorUsuario(correo)
    }
}

/**
 * Función de extensión para transformar la entidad relacional TruequeConOfertas en el modelo de dominio TradeProposal.
 */
fun TruequeConOfertas.toTradeProposal(): TradeProposal {
    val ofA = ofertaA
    val ofB = ofertaB
    val offeredList = ofA?.articulosIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val target = ofB?.articulosIds?.split(",")?.firstOrNull() ?: ""
    val coins = ofA?.truequeCoins ?: 0.0
    val statusEnum = try {
        TradeStatus.valueOf(trueque.estado)
    } catch (e: Exception) {
        TradeStatus.PENDIENTE
    }

    val modality = when {
        coins > 0.0 && offeredList.isNotEmpty() -> TradeModality.HIBRIDO_CON_CREDITOS
        coins > 0.0 -> TradeModality.PURO_TRUEQUECOINS
        offeredList.size > 1 -> TradeModality.COMPUESTO_N_A_1
        else -> TradeModality.DIRECTO_1_A_1
    }

    return TradeProposal(
        id = trueque.id,
        requesterUserId = trueque.usuarioACorreo,
        receiverUserId = trueque.usuarioBCorreo,
        offeredItemIds = offeredList,
        targetItemId = target,
        compensatoryTruequeCoins = coins,
        modality = modality,
        status = statusEnum,
        createdAtEpochMs = trueque.fechaCreacion
    )
}
