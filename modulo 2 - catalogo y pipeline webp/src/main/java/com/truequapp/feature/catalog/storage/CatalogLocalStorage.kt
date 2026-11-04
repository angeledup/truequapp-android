package com.truequapp.feature.catalog.storage

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
 * Almacenamiento local persistente para las publicaciones del usuario y artículos marcados como favoritos (Fase 2).
 * Resguarda los artículos publicados en el almacenamiento interno de la app (sandbox) para no perderlos tras reinicios.
 */
@Singleton
class CatalogLocalStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val publicationsFile: File
        get() = File(context.filesDir, "user_published_items.json")

    private val bookmarksFile: File
        get() = File(context.filesDir, "user_bookmarked_items.json")

    /**
     * Guarda la lista de artículos publicados por el usuario de forma persistente.
     */
    fun savePublishedItems(items: List<TradeItem>) {
        try {
            val jsonArray = JSONArray()
            items.forEach { item ->
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
            publicationsFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Recupera los artículos publicados por el usuario desde el almacenamiento local.
     */
    fun loadPublishedItems(): List<TradeItem> {
        if (!publicationsFile.exists()) return emptyList()
        return try {
            val jsonStr = publicationsFile.readText()
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
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Guarda el conjunto de identificadores de artículos marcados / favoritos por el usuario.
     */
    fun saveBookmarkedIds(ids: Set<String>) {
        try {
            val jsonArray = JSONArray(ids.toList())
            bookmarksFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Carga el conjunto de identificadores de artículos marcados / favoritos.
     */
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

