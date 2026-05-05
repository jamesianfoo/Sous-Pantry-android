package com.souspantry.app.data.repository

import android.content.Context
import androidx.room.*
import com.souspantry.app.data.models.PantryItem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

// ── DAO ───────────────────────────────────────────────────────────────────────

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

@Database(entities = [PantryItem::class], version = 1, exportSchema = false)
abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
}

// ── Hilt module ───────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun providePantryDatabase(@ApplicationContext ctx: Context): PantryDatabase =
        Room.databaseBuilder(ctx, PantryDatabase::class.java, "sous_pantry.db").build()

    @Provides @Singleton
    fun providePantryDao(db: PantryDatabase): PantryDao = db.pantryDao()
}
