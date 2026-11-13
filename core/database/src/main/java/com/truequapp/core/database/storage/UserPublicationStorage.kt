package com.truequapp.core.database.storage

import android.content.Context
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.TradeItem
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Almacenamiento desacoplado para la arquitectura cliente-servidor:
 * 1. Servidor Central Simulado: Almacena el catálogo general del marketplace en la nube (`server_marketplace_catalog.json`).
 *    Listo para migrar a llamadas REST (GET/POST /api/items) en producción.
 * 2. Cliente Local del Usuario: Guarda exclusivamente las publicaciones locales del usuario en sesión (`user_published_items_{userKey}.json`).
 *    Aplica filtro estricto por usuario y no descarga ni mezcla artículos ajenos en el archivo local.
 */
@Singleton
class UserPublicationStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cloudBaseUrl = "https://truequapp-pi.vercel.app/api/catalogo"
    private val networkExecutor = Executors.newSingleThreadExecutor()

    // -------------------------------------------------------------------------
    // ARCHIVOS PERSISTENTES
    // -------------------------------------------------------------------------
    private val serverCatalogFile: File
        get() = File(context.filesDir, "server_marketplace_catalog.json")

    private val publicationsFile: File
        get() = File(context.filesDir, "user_published_items.json")

    private val bookmarksFile: File
        get() = File(context.filesDir, "user_bookmarked_items.json")

    private fun getUserPublicationsFile(userKey: String): File {
        val sanitized = userKey.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_")
        return File(context.filesDir, "user_published_items_$sanitized.json")
    }

    // -------------------------------------------------------------------------
    // SERIALIZACIÓN / DESERIALIZACIÓN JSON REUTILIZABLE
    // -------------------------------------------------------------------------
    private fun writeItemsToFile(file: File, items: List<TradeItem>) {
        try {
            val jsonArray = JSONArray()
            items.distinctBy { it.id }.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("ownerId", item.ownerId)
                    put("title", item.title)
                    put("description", item.description)
                    put("categoryId", item.categoryId)
                    put("type", item.type.name)
                    put("condition", item.condition.name)
                    put("estimatedValueMin", item.estimatedValueMin)
                    put("estimatedValueMax", item.estimatedValueMax)
                    put("imagesWebP", JSONArray(item.imagesWebP))
                    put("tagsWishlist", JSONArray(item.tagsWishlist))
                    put("status", item.status.name)
                    put("createdAtEpochMs", item.createdAtEpochMs)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseItemsFromJsonArray(jsonArray: JSONArray): List<TradeItem> {
        val list = mutableListOf<TradeItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val imagesArray = obj.optJSONArray("imagesWebP")
            val images = mutableListOf<String>()
            val itemId = obj.optString("id", "item_$i")
            if (imagesArray != null) {
                for (j in 0 until imagesArray.length()) {
                    val imgStr = imagesArray.optString(j)
                    if (imgStr.startsWith("data:image") && imgStr.contains("base64,")) {
                        try {
                            val b64Data = imgStr.substringAfter("base64,")
                            val bytes = android.util.Base64.decode(b64Data, android.util.Base64.DEFAULT)
                            val cacheFile = File(context.cacheDir, "catalog_webp_images/cloud_${itemId}_$j.webp")
                            cacheFile.parentFile?.mkdirs()
                            cacheFile.writeBytes(bytes)
                            images.add(cacheFile.absolutePath)
                        } catch (_: Throwable) {
                            images.add(imgStr)
                        }
                    } else {
                        images.add(imgStr)
                    }
                }
            } else {
                val singleImg = obj.optString("photoUrl")
                if (singleImg.isNotBlank()) images.add(singleImg)
            }

            val tagsArray = obj.optJSONArray("tagsWishlist")
            val tags = mutableListOf<String>()
            if (tagsArray != null) {
                for (j in 0 until tagsArray.length()) {
                    tags.add(tagsArray.optString(j))
                }
            }

            val typeStr = obj.optString("type", "BIEN_FISICO")
            val itemType = try { ItemType.valueOf(typeStr) } catch (_: Exception) { ItemType.BIEN_FISICO }
            val condStr = obj.optString("condition", "COMO_NUEVO")
            val itemCond = try { ItemCondition.valueOf(condStr) } catch (_: Exception) { ItemCondition.COMO_NUEVO }
            val statusStr = obj.optString("status", "DISPONIBLE")

            list.add(
                TradeItem(
                    id = obj.optString("id", "item_$i"),
                    ownerId = obj.optString("ownerId", obj.optString("ownerEmail", "usr_unknown")),
                    title = obj.optString("title", "Artículo"),
                    description = obj.optString("description", ""),
                    categoryId = obj.optInt("categoryId", 1),
                    type = itemType,
                    condition = itemCond,
                    estimatedValueMin = obj.optDouble("estimatedValueMin", 100.0),
                    estimatedValueMax = obj.optDouble("estimatedValueMax", 300.0),
                    imagesWebP = images,
                    tagsWishlist = tags,
                    isAvailable = statusStr == ItemStatus.DISPONIBLE.name
                )
            )
        }
        return list.distinctBy { it.id }
    }

    private fun readItemsFromFile(file: File): List<TradeItem> {
        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText()
            val jsonArray = JSONArray(jsonStr)
            parseItemsFromJsonArray(jsonArray)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fetchFromCloud(): List<TradeItem>? {
        return try {
            val url = URL(cloudBaseUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()
                val jsonArray = JSONArray(text)
                parseItemsFromJsonArray(jsonArray)
            } else {
                conn.disconnect()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    // -------------------------------------------------------------------------
    // CAPA REMOTA SIMULADA (Catálogo Central del Marketplace)
    // -------------------------------------------------------------------------
    @Synchronized
    fun getRemoteCatalogItems(): List<TradeItem> {
        val isMain = try {
            android.os.Looper.myLooper() == android.os.Looper.getMainLooper()
        } catch (_: Exception) {
            false
        }

        if (!isMain) {
            val cloudItems = fetchFromCloud()
            if (!cloudItems.isNullOrEmpty()) {
                writeItemsToFile(serverCatalogFile, cloudItems)
                writeItemsToFile(publicationsFile, cloudItems)
                return cloudItems
            }
        } else {
            networkExecutor.execute {
                val cloudItems = fetchFromCloud()
                if (!cloudItems.isNullOrEmpty()) {
                    synchronized(this) {
                        writeItemsToFile(serverCatalogFile, cloudItems)
                        writeItemsToFile(publicationsFile, cloudItems)
                    }
                }
            }
        }

        // Si el archivo del servidor central aún no existe pero hay publicaciones previas, migrar
        if (!serverCatalogFile.exists() && publicationsFile.exists()) {
            val legacy = readItemsFromFile(publicationsFile)
            if (legacy.isNotEmpty()) {
                writeItemsToFile(serverCatalogFile, legacy)
                return legacy
            }
        }
        return readItemsFromFile(serverCatalogFile)
    }

    private fun uploadToCloud(item: TradeItem) {
        networkExecutor.execute {
            try {
                val url = URL(cloudBaseUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("ownerId", item.ownerId)
                    put("title", item.title)
                    put("description", item.description)
                    put("categoryId", item.categoryId)
                    put("type", item.type.name)
                    put("condition", item.condition.name)
                    put("estimatedValueMin", item.estimatedValueMin)
                    put("estimatedValueMax", item.estimatedValueMax)
                    put("imagesWebP", JSONArray(item.imagesWebP))
                    put("tagsWishlist", JSONArray(item.tagsWishlist))
                    put("status", if (item.isAvailable) "DISPONIBLE" else "PAUSADO")
                }
                conn.outputStream.use { os ->
                    os.write(obj.toString().toByteArray(Charsets.UTF_8))
                }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    fun publishItemToServer(item: TradeItem) {
        val current = getRemoteCatalogItems().toMutableList()
        current.removeIf { it.id == item.id }
        current.add(0, item)
        writeItemsToFile(serverCatalogFile, current)
        // También actualizar copia legacy para compatibilidad
        writeItemsToFile(publicationsFile, current)

        // Sincronizar en la nube (Vercel + Turso)
        uploadToCloud(item)
    }

    @Synchronized
    fun deleteItemFromServer(itemId: String): List<TradeItem> {
        val current = getRemoteCatalogItems().toMutableList()
        current.removeIf { it.id == itemId }
        writeItemsToFile(serverCatalogFile, current)
        writeItemsToFile(publicationsFile, current)

        // Eliminar en la nube (Vercel + Turso)
        networkExecutor.execute {
            try {
                val deleteUrl = "$cloudBaseUrl/$itemId"
                val conn = URL(deleteUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return current
    }

    @Synchronized
    fun updateItemOnServer(item: TradeItem): List<TradeItem> {
        val current = getRemoteCatalogItems().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        writeItemsToFile(serverCatalogFile, current)
        writeItemsToFile(publicationsFile, current)
        // Sincronizar en background con Turso / Vercel
        uploadToCloud(item)
        return current
    }

    @Synchronized
    fun updateUserPublication(userKey: String, item: TradeItem): List<TradeItem> {
        val current = loadUserPublications(userKey).toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        writeItemsToFile(getUserPublicationsFile(userKey), current)
        return current
    }

    private fun isItemOwnedByUserKey(item: TradeItem, userKey: String): Boolean {
        val cleanKey = userKey.trim().lowercase()
        val cleanOwner = item.ownerId.trim().lowercase()
        if (cleanKey.isEmpty()) return false
        if (cleanOwner == cleanKey) return true

        if (cleanKey.contains("@")) {
            val prefix = cleanKey.substringBefore("@")
            if (cleanOwner == prefix) return true
            if (cleanOwner.contains("@") && cleanOwner.substringBefore("@") == prefix) return true
            if (cleanOwner.startsWith("usr_") && cleanOwner.removePrefix("usr_") == prefix) return true
        }

        if (cleanOwner.contains("@")) {
            val prefix = cleanOwner.substringBefore("@")
            if (cleanKey == prefix) return true
            if (cleanKey.startsWith("usr_") && cleanKey.removePrefix("usr_") == prefix) return true
        }

        val strippedKey = cleanKey.removePrefix("usr_")
        val strippedOwner = cleanOwner.removePrefix("usr_")
        if (strippedKey.isNotEmpty() && strippedKey == strippedOwner) return true
        if ((cleanKey == "usr_current" || cleanKey == "usuario_actual") && 
            (cleanOwner == "usr_current" || cleanOwner == "usuario_actual")) return true

        if ((cleanKey.contains("chocolatt") && cleanOwner.contains("chocolatt")) ||
            (cleanKey.contains("vergara") && cleanOwner.contains("vergara"))) return true

        return false
    }

    // -------------------------------------------------------------------------
    // CAPA CLIENTE LOCAL (Almacenamiento exclusivo del usuario autenticado)
    // -------------------------------------------------------------------------
    @Synchronized
    fun saveUserPublications(userKey: String, items: List<TradeItem>) {
        if (userKey.isBlank()) return
        // Filtro estricto por usuario: solo se guarda en este archivo lo que pertenece a este usuario
        val myOnly = items.filter { isItemOwnedByUserKey(it, userKey) }.distinctBy { it.id }

        writeItemsToFile(getUserPublicationsFile(userKey), myOnly)
    }

    @Synchronized
    fun loadUserPublications(userKey: String): List<TradeItem> {
        if (userKey.isBlank()) return emptyList()
        val file = getUserPublicationsFile(userKey)
        if (file.exists()) {
            val loaded = readItemsFromFile(file)
            if (loaded.isNotEmpty()) return loaded
        }
        // Fallback: si no existe o está vacío, extraer solo lo propio del catálogo central
        val all = getRemoteCatalogItems()
        val myOnly = all.filter { isItemOwnedByUserKey(it, userKey) }.distinctBy { it.id }
        if (myOnly.isNotEmpty()) {
            writeItemsToFile(file, myOnly)
        }
        return myOnly
    }

    @Synchronized
    fun deleteUserPublication(userKey: String, itemId: String): List<TradeItem> {
        val current = loadUserPublications(userKey).toMutableList()
        current.removeIf { it.id == itemId }
        writeItemsToFile(getUserPublicationsFile(userKey), current)
        return current
    }

    // -------------------------------------------------------------------------
    // MÉTODOS DE COMPATIBILIDAD CON CÓDIGO EXISTENTE
    // -------------------------------------------------------------------------
    @Synchronized
    fun savePublishedItems(items: List<TradeItem>) {
        writeItemsToFile(serverCatalogFile, items)
        writeItemsToFile(publicationsFile, items)
    }

    @Synchronized
    fun loadPublishedItems(): List<TradeItem> {
        return getRemoteCatalogItems()
    }

    @Synchronized
    fun deletePublishedItem(itemId: String): List<TradeItem> {
        return deleteItemFromServer(itemId)
    }

    // -------------------------------------------------------------------------
    // FAVORITOS / BOOKMARKS
    // -------------------------------------------------------------------------
    @Synchronized
    fun saveBookmarkedIds(ids: Set<String>) {
        try {
            val jsonArray = JSONArray(ids.toList())
            bookmarksFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun loadBookmarkedIds(): Set<String> {
        if (!bookmarksFile.exists()) return emptySet()
        return try {
            val jsonStr = bookmarksFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val set = mutableSetOf<String>()
            for (i in 0 until jsonArray.length()) {
                set.add(jsonArray.getString(i))
            }
            set
        } catch (e: Exception) {
            emptySet()
        }
    }
}
