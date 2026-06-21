package com.souspantry.app.data.repository

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.souspantry.app.data.models.PantryItem
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

    @Query("SELECT * FROM pantry_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PantryItem?
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(
    entities = [PantryItem::class, WeekMealEntry::class, MyRecipe::class, SavedRecipe::class],
    version  = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun weekMealDao(): WeekMealDao
    abstract fun myRecipeDao(): MyRecipeDao
    abstract fun savedRecipeDao(): SavedRecipeDao
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

// ── Hilt module ───────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun providePantryDatabase(@ApplicationContext ctx: Context): PantryDatabase =
        Room.databaseBuilder(ctx, PantryDatabase::class.java, "sous_pantry.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides @Singleton
    fun providePantryDao(db: PantryDatabase): PantryDao = db.pantryDao()

    @Provides @Singleton
    fun provideWeekMealDao(db: PantryDatabase): WeekMealDao = db.weekMealDao()

    @Provides @Singleton
    fun provideMyRecipeDao(db: PantryDatabase): MyRecipeDao = db.myRecipeDao()

    @Provides @Singleton
    fun provideSavedRecipeDao(db: PantryDatabase): SavedRecipeDao = db.savedRecipeDao()
}
