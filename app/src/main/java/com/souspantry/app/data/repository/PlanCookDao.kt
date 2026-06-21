package com.souspantry.app.data.repository

import androidx.room.*
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
}

@Dao
interface MyRecipeDao {
    @Query("SELECT * FROM my_recipes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MyRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: MyRecipe)

    @Query("DELETE FROM my_recipes WHERE id = :id")
    suspend fun deleteById(id: String)
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
}

// ── Repositories ────────────────────────────────────────────────────────────

@Singleton
class WeekPlanRepository @Inject constructor(private val dao: WeekMealDao) {
    val entries: Flow<List<WeekMealEntry>> = dao.getAll()
    suspend fun upsert(entry: WeekMealEntry) = dao.upsert(entry)
    suspend fun delete(id: String)           = dao.deleteById(id)
}

@Singleton
class MyRecipeRepository @Inject constructor(private val dao: MyRecipeDao) {
    val recipes: Flow<List<MyRecipe>> = dao.getAll()
    suspend fun upsert(recipe: MyRecipe) = dao.upsert(recipe)
    suspend fun delete(id: String)        = dao.deleteById(id)
}

@Singleton
class SavedRecipeRepository @Inject constructor(private val dao: SavedRecipeDao) {
    val recipes: Flow<List<SavedRecipe>> = dao.getAll()
    suspend fun upsert(recipe: SavedRecipe)   = dao.upsert(recipe)
    suspend fun delete(id: String)            = dao.deleteById(id)
    suspend fun deleteByTitle(title: String)  = dao.deleteByTitle(title)
    suspend fun isSaved(title: String): Boolean = dao.countByTitle(title) > 0
}
