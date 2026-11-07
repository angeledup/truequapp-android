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

    private fun readItemsFromFile(file: File): List<TradeItem> {
        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<TradeItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val imagesArray = obj.optJSONArray("imagesWebP")
                val images = mutableListOf<String>()
                if (imagesArray != null) {
                    for (j in 0 until imagesArray.length()) {
                        images.add(imagesArray.getString(j))
                    }
                }

                val tagsArray = obj.optJSONArray("tagsWishlist")
                val tags = mutableListOf<String>()
                if (tagsArray != null) {
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                }

                list.add(
                    TradeItem(
                        id = obj.getString("id"),
                        ownerId = obj.getString("ownerId"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        categoryId = obj.getInt("categoryId"),
                        type = ItemType.valueOf(obj.getString("type")),
                        condition = ItemCondition.valueOf(obj.getString("condition")),
                        estimatedValueMin = obj.getDouble("estimatedValueMin"),
                        estimatedValueMax = obj.getDouble("estimatedValueMax"),
                        imagesWebP = images,
                        tagsWishlist = tags,
                        isAvailable = obj.optString("status") == ItemStatus.DISPONIBLE.name
                    )
                )
            }
            list.distinctBy { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // -------------------------------------------------------------------------
    // CAPA REMOTA SIMULADA (Catálogo Central del Marketplace)
    // -------------------------------------------------------------------------
    @Synchronized
    fun getRemoteCatalogItems(): List<TradeItem> {
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

    @Synchronized
    fun publishItemToServer(item: TradeItem) {
        val current = getRemoteCatalogItems().toMutableList()
        current.removeIf { it.id == item.id }
        current.add(0, item)
        writeItemsToFile(serverCatalogFile, current)
        // También actualizar copia legacy para compatibilidad
        writeItemsToFile(publicationsFile, current)
    }

    @Synchronized
    fun deleteItemFromServer(itemId: String): List<TradeItem> {
        val current = getRemoteCatalogItems().toMutableList()
        current.removeIf { it.id == itemId }
        writeItemsToFile(serverCatalogFile, current)
        writeItemsToFile(publicationsFile, current)
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
        return current
    }

    // -------------------------------------------------------------------------
    // CAPA CLIENTE LOCAL (Almacenamiento exclusivo del usuario autenticado)
    // -------------------------------------------------------------------------
    @Synchronized
    fun saveUserPublications(userKey: String, items: List<TradeItem>) {
        if (userKey.isBlank()) return
        // Filtro estricto por usuario: solo se guarda en este archivo lo que pertenece a este usuario
        val myOnly = items.filter {
            it.ownerId.equals(userKey, ignoreCase = true) ||
            (userKey.contains("@") && it.ownerId.equals(userKey.substringBefore("@"), ignoreCase = true))
        }.distinctBy { it.id }

        writeItemsToFile(getUserPublicationsFile(userKey), myOnly)
    }

    @Synchronized
    fun loadUserPublications(userKey: String): List<TradeItem> {
        if (userKey.isBlank()) return emptyList()
        val file = getUserPublicationsFile(userKey)
        if (file.exists()) {
            return readItemsFromFile(file)
        }
        // Fallback: si no existe el archivo específico, extraer solo lo propio del catálogo central
        val all = getRemoteCatalogItems()
        val myOnly = all.filter {
            it.ownerId.equals(userKey, ignoreCase = true) ||
            (userKey.contains("@") && it.ownerId.equals(userKey.substringBefore("@"), ignoreCase = true))
        }
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
