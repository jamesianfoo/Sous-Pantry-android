package com.souspantry.app.data.repository

import com.souspantry.app.data.models.PantryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PantryRepository @Inject constructor(
    private val dao: PantryDao,
) {
    val items: Flow<List<PantryItem>> = dao.getAllItems()

    suspend fun add(item: PantryItem)           = dao.insert(item)
    suspend fun addAll(items: List<PantryItem>) = dao.insertAll(items)
    suspend fun update(item: PantryItem)        = dao.update(item)
    suspend fun delete(item: PantryItem)        = dao.delete(item)
    suspend fun deleteAll()                     = dao.deleteAll()
}
