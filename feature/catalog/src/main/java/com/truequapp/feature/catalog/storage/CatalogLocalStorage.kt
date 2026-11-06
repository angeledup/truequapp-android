package com.truequapp.feature.catalog.storage

import android.content.Context
import com.truequapp.core.database.storage.UserPublicationStorage
import com.truequapp.core.domain.model.TradeItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adaptador de almacenamiento local de catálogo que delega en el almacenamiento unificado [UserPublicationStorage].
 */
@Singleton
class CatalogLocalStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val delegate: UserPublicationStorage
) {
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
