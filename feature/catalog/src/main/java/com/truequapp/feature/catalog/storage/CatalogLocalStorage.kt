package com.truequapp.feature.catalog.storage

import android.content.Context
import com.truequapp.core.database.storage.UserPublicationStorage
import com.truequapp.core.domain.model.TradeItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adaptador de almacenamiento local de catálogo que delega en el almacenamiento unificado [UserPublicationStorage].
 * Separa la capa del servidor simulado (catálogo general) del almacenamiento local exclusivo del usuario.
 */
@Singleton
class CatalogLocalStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val delegate: UserPublicationStorage
) {
    // --- SERVIDOR CENTRAL SIMULADO (Marketplace Remoto) ---
    fun getRemoteCatalogItems(): List<TradeItem> = delegate.getRemoteCatalogItems()

    fun publishItemToServer(item: TradeItem) = delegate.publishItemToServer(item)

    fun deleteItemFromServer(itemId: String): List<TradeItem> = delegate.deleteItemFromServer(itemId)

    fun updateItemOnServer(item: TradeItem): List<TradeItem> = delegate.updateItemOnServer(item)

    // --- ALMACENAMIENTO LOCAL EXCLUSIVO DEL USUARIO ---
    fun saveUserPublications(userKey: String, items: List<TradeItem>) = delegate.saveUserPublications(userKey, items)

    fun loadUserPublications(userKey: String): List<TradeItem> = delegate.loadUserPublications(userKey)

    fun deleteUserPublication(userKey: String, itemId: String): List<TradeItem> = delegate.deleteUserPublication(userKey, itemId)

    // --- COMPATIBILIDAD ---
    fun savePublishedItems(items: List<TradeItem>) {
        delegate.savePublishedItems(items)
    }

    fun loadPublishedItems(): List<TradeItem> {
        return delegate.loadPublishedItems()
    }

    fun deletePublishedItem(itemId: String): List<TradeItem> {
        return delegate.deletePublishedItem(itemId)
    }

    fun saveBookmarkedIds(ids: Set<String>) {
        delegate.saveBookmarkedIds(ids)
    }

    fun loadBookmarkedIds(): Set<String> {
        return delegate.loadBookmarkedIds()
    }
}
