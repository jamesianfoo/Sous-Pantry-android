package com.souspantry.app.data.repository

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.ui.plancook.MealHistorySession
import com.souspantry.app.ui.plancook.MyRecipe
import com.souspantry.app.ui.plancook.SavedRecipe
import com.souspantry.app.ui.plancook.WeekMealEntry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

// ── Pantry DAO ──────────────────────────────────────────────────────────────

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY dateAdded DESC")
    fun getAllItems(): Flow<List<PantryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PantryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PantryItem>)

    @Update
    suspend fun update(item: PantryItem)

    @Delete
    suspend fun delete(item: PantryItem)

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pantry_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM pantry_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PantryItem?
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(
    entities = [PantryItem::class, WeekMealEntry::class, MyRecipe::class, SavedRecipe::class, ShoppingItem::class, MealHistorySession::class],
    version  = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun weekMealDao(): WeekMealDao
    abstract fun myRecipeDao(): MyRecipeDao
    abstract fun savedRecipeDao(): SavedRecipeDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun mealHistoryDao(): MealHistoryDao
}

/**
 * v1 → v2: add the Plan & Cook tables. pantry_items is left untouched so the
 * user's existing pantry survives the upgrade (no destructive fallback).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS week_meal_entries (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                cuisine TEXT NOT NULL,
                prepTime TEXT NOT NULL,
                cookTime TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                ingredients TEXT NOT NULL,
                instructions TEXT NOT NULL,
                scheduledDate INTEGER NOT NULL,
                isUserAdded INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS my_recipes (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                cuisine TEXT NOT NULL,
                prepTime TEXT NOT NULL,
                cookTime TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                ingredients TEXT NOT NULL,
                instructions TEXT NOT NULL,
                servings INTEGER NOT NULL,
                notes TEXT NOT NULL,
                source TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_recipes (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                cuisine TEXT NOT NULL,
                prepTime TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                ingredients TEXT NOT NULL,
                instructions TEXT NOT NULL,
                savedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * v2 → v3: add the shopping_items table so the Shopping list (and items pushed
 * to it from recipe details) persists across restarts. Existing tables untouched.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS shopping_items (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT,
                quantity TEXT,
                priority TEXT NOT NULL,
                reason TEXT,
                checked INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * v3 → v4: add the meal_history_sessions table so Discover's History sheet
 * persists past generations across restarts. Existing tables untouched.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS meal_history_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                createdAt INTEGER NOT NULL,
                cuisines TEXT NOT NULL,
                meals TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

// ── Hilt module ───────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun providePantryDatabase(@ApplicationContext ctx: Context): PantryDatabase =
        Room.databaseBuilder(ctx, PantryDatabase::class.java, "sous_pantry.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides @Singleton
    fun providePantryDao(db: PantryDatabase): PantryDao = db.pantryDao()

    @Provides @Singleton
    fun provideWeekMealDao(db: PantryDatabase): WeekMealDao = db.weekMealDao()

    @Provides @Singleton
    fun provideMyRecipeDao(db: PantryDatabase): MyRecipeDao = db.myRecipeDao()

    @Provides @Singleton
    fun provideSavedRecipeDao(db: PantryDatabase): SavedRecipeDao = db.savedRecipeDao()

    @Provides @Singleton
    fun provideShoppingDao(db: PantryDatabase): ShoppingDao = db.shoppingDao()

    @Provides @Singleton
    fun provideMealHistoryDao(db: PantryDatabase): MealHistoryDao = db.mealHistoryDao()
}
