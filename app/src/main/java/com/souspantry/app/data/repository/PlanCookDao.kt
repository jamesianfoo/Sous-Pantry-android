package com.souspantry.app.data.repository

import androidx.room.*
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.ui.plancook.MyRecipe
import com.souspantry.app.ui.plancook.SavedRecipe
import com.souspantry.app.ui.plancook.WeekMealEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// ── DAOs ────────────────────────────────────────────────────────────────────

@Dao
interface WeekMealDao {
    @Query("SELECT * FROM week_meal_entries ORDER BY scheduledDate ASC")
    fun getAll(): Flow<List<WeekMealEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeekMealEntry)

    @Query("DELETE FROM week_meal_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM week_meal_entries")
    suspend fun deleteAll()
}

@Dao
interface MyRecipeDao {
    @Query("SELECT * FROM my_recipes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MyRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: MyRecipe)

    @Query("DELETE FROM my_recipes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM my_recipes")
    suspend fun deleteAll()
}

@Dao
interface SavedRecipeDao {
    @Query("SELECT * FROM saved_recipes ORDER BY savedAt DESC")
    fun getAll(): Flow<List<SavedRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: SavedRecipe)

    @Query("DELETE FROM saved_recipes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM saved_recipes WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title))")
    suspend fun deleteByTitle(title: String)

    @Query("SELECT COUNT(*) FROM saved_recipes WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title))")
    suspend fun countByTitle(title: String): Int

    @Query("DELETE FROM saved_recipes")
    suspend fun deleteAll()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items")
    fun getAll(): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShoppingItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ShoppingItem>)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun deleteChecked()

    @Query("SELECT * FROM shopping_items WHERE checked = 1")
    suspend fun getChecked(): List<ShoppingItem>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun countByName(name: String): Int

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}

// ── Repositories ────────────────────────────────────────────────────────────

@Singleton
class WeekPlanRepository @Inject constructor(private val dao: WeekMealDao) {
    val entries: Flow<List<WeekMealEntry>> = dao.getAll()
    suspend fun upsert(entry: WeekMealEntry) = dao.upsert(entry)
    suspend fun delete(id: String)           = dao.deleteById(id)
    suspend fun deleteAll()                  = dao.deleteAll()
}

@Singleton
class MyRecipeRepository @Inject constructor(private val dao: MyRecipeDao) {
    val recipes: Flow<List<MyRecipe>> = dao.getAll()
    suspend fun upsert(recipe: MyRecipe) = dao.upsert(recipe)
    suspend fun delete(id: String)        = dao.deleteById(id)
    suspend fun deleteAll()               = dao.deleteAll()
}

@Singleton
class SavedRecipeRepository @Inject constructor(private val dao: SavedRecipeDao) {
    val recipes: Flow<List<SavedRecipe>> = dao.getAll()
    suspend fun upsert(recipe: SavedRecipe)   = dao.upsert(recipe)
    suspend fun delete(id: String)            = dao.deleteById(id)
    suspend fun deleteByTitle(title: String)  = dao.deleteByTitle(title)
    suspend fun isSaved(title: String): Boolean = dao.countByTitle(title) > 0
    suspend fun deleteAll()                   = dao.deleteAll()
}

@Singleton
class ShoppingRepository @Inject constructor(private val dao: ShoppingDao) {
    val items: Flow<List<ShoppingItem>> = dao.getAll()
    suspend fun upsert(item: ShoppingItem)            = dao.upsert(item)
    suspend fun upsertAll(items: List<ShoppingItem>)  = dao.upsertAll(items)
    suspend fun delete(id: String)                    = dao.deleteById(id)
    suspend fun deleteChecked()                       = dao.deleteChecked()
    suspend fun getChecked(): List<ShoppingItem>      = dao.getChecked()
    suspend fun exists(name: String): Boolean         = dao.countByName(name) > 0
    suspend fun deleteAll()                           = dao.deleteAll()
}
