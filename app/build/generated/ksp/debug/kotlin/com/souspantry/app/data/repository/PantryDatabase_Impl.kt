package com.souspantry.app.`data`.repository

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PantryDatabase_Impl : PantryDatabase() {
  private val _pantryDao: Lazy<PantryDao> = lazy {
    PantryDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "25ce856114bfd3fc1df3dc2d50e5fe19", "1fea179e72bb7cce4b67ec531979f250") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `pantry_items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `brand` TEXT, `category` TEXT, `quantity` INTEGER NOT NULL, `notes` TEXT, `expiryDate` INTEGER, `dateAdded` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '25ce856114bfd3fc1df3dc2d50e5fe19')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `pantry_items`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsPantryItems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPantryItems.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPantryItems.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPantryItems.put("brand", TableInfo.Column("brand", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPantryItems.put("category", TableInfo.Column("category", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPantryItems.put("quantity", TableInfo.Column("quantity", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPantryItems.put("notes", TableInfo.Column("notes", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPantryItems.put("expiryDate", TableInfo.Column("expiryDate", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPantryItems.put("dateAdded", TableInfo.Column("dateAdded", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPantryItems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPantryItems: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPantryItems: TableInfo = TableInfo("pantry_items", _columnsPantryItems,
            _foreignKeysPantryItems, _indicesPantryItems)
        val _existingPantryItems: TableInfo = read(connection, "pantry_items")
        if (!_infoPantryItems.equals(_existingPantryItems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |pantry_items(com.souspantry.app.data.models.PantryItem).
              | Expected:
              |""".trimMargin() + _infoPantryItems + """
              |
              | Found:
              |""".trimMargin() + _existingPantryItems)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "pantry_items")
  }

  public override fun clearAllTables() {
    super.performClear(false, "pantry_items")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(PantryDao::class, PantryDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun pantryDao(): PantryDao = _pantryDao.value
}
