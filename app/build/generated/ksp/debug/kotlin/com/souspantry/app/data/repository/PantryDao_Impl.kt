package com.souspantry.app.`data`.repository

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.souspantry.app.`data`.models.PantryItem
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PantryDao_Impl(
  __db: RoomDatabase,
) : PantryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPantryItem: EntityInsertAdapter<PantryItem>

  private val __deleteAdapterOfPantryItem: EntityDeleteOrUpdateAdapter<PantryItem>

  private val __updateAdapterOfPantryItem: EntityDeleteOrUpdateAdapter<PantryItem>
  init {
    this.__db = __db
    this.__insertAdapterOfPantryItem = object : EntityInsertAdapter<PantryItem>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `pantry_items` (`id`,`name`,`brand`,`category`,`quantity`,`notes`,`expiryDate`,`dateAdded`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PantryItem) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmpBrand: String? = entity.brand
        if (_tmpBrand == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpBrand)
        }
        val _tmpCategory: String? = entity.category
        if (_tmpCategory == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpCategory)
        }
        statement.bindLong(5, entity.quantity.toLong())
        val _tmpNotes: String? = entity.notes
        if (_tmpNotes == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpNotes)
        }
        val _tmpExpiryDate: Long? = entity.expiryDate
        if (_tmpExpiryDate == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpExpiryDate)
        }
        statement.bindLong(8, entity.dateAdded)
      }
    }
    this.__deleteAdapterOfPantryItem = object : EntityDeleteOrUpdateAdapter<PantryItem>() {
      protected override fun createQuery(): String = "DELETE FROM `pantry_items` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PantryItem) {
        statement.bindText(1, entity.id)
      }
    }
    this.__updateAdapterOfPantryItem = object : EntityDeleteOrUpdateAdapter<PantryItem>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `pantry_items` SET `id` = ?,`name` = ?,`brand` = ?,`category` = ?,`quantity` = ?,`notes` = ?,`expiryDate` = ?,`dateAdded` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PantryItem) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmpBrand: String? = entity.brand
        if (_tmpBrand == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpBrand)
        }
        val _tmpCategory: String? = entity.category
        if (_tmpCategory == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpCategory)
        }
        statement.bindLong(5, entity.quantity.toLong())
        val _tmpNotes: String? = entity.notes
        if (_tmpNotes == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpNotes)
        }
        val _tmpExpiryDate: Long? = entity.expiryDate
        if (_tmpExpiryDate == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpExpiryDate)
        }
        statement.bindLong(8, entity.dateAdded)
        statement.bindText(9, entity.id)
      }
    }
  }

  public override suspend fun insert(item: PantryItem): Unit = performSuspending(__db, false, true)
      { _connection ->
    __insertAdapterOfPantryItem.insert(_connection, item)
  }

  public override suspend fun insertAll(items: List<PantryItem>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfPantryItem.insert(_connection, items)
  }

  public override suspend fun delete(item: PantryItem): Unit = performSuspending(__db, false, true)
      { _connection ->
    __deleteAdapterOfPantryItem.handle(_connection, item)
  }

  public override suspend fun update(item: PantryItem): Unit = performSuspending(__db, false, true)
      { _connection ->
    __updateAdapterOfPantryItem.handle(_connection, item)
  }

  public override fun getAllItems(): Flow<List<PantryItem>> {
    val _sql: String = "SELECT * FROM pantry_items ORDER BY dateAdded DESC"
    return createFlow(__db, false, arrayOf("pantry_items")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBrand: Int = getColumnIndexOrThrow(_stmt, "brand")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfExpiryDate: Int = getColumnIndexOrThrow(_stmt, "expiryDate")
        val _columnIndexOfDateAdded: Int = getColumnIndexOrThrow(_stmt, "dateAdded")
        val _result: MutableList<PantryItem> = mutableListOf()
        while (_stmt.step()) {
          val _item: PantryItem
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBrand: String?
          if (_stmt.isNull(_columnIndexOfBrand)) {
            _tmpBrand = null
          } else {
            _tmpBrand = _stmt.getText(_columnIndexOfBrand)
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpNotes: String?
          if (_stmt.isNull(_columnIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          }
          val _tmpExpiryDate: Long?
          if (_stmt.isNull(_columnIndexOfExpiryDate)) {
            _tmpExpiryDate = null
          } else {
            _tmpExpiryDate = _stmt.getLong(_columnIndexOfExpiryDate)
          }
          val _tmpDateAdded: Long
          _tmpDateAdded = _stmt.getLong(_columnIndexOfDateAdded)
          _item =
              PantryItem(_tmpId,_tmpName,_tmpBrand,_tmpCategory,_tmpQuantity,_tmpNotes,_tmpExpiryDate,_tmpDateAdded)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findById(id: String): PantryItem? {
    val _sql: String = "SELECT * FROM pantry_items WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfBrand: Int = getColumnIndexOrThrow(_stmt, "brand")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfExpiryDate: Int = getColumnIndexOrThrow(_stmt, "expiryDate")
        val _columnIndexOfDateAdded: Int = getColumnIndexOrThrow(_stmt, "dateAdded")
        val _result: PantryItem?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpBrand: String?
          if (_stmt.isNull(_columnIndexOfBrand)) {
            _tmpBrand = null
          } else {
            _tmpBrand = _stmt.getText(_columnIndexOfBrand)
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpNotes: String?
          if (_stmt.isNull(_columnIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          }
          val _tmpExpiryDate: Long?
          if (_stmt.isNull(_columnIndexOfExpiryDate)) {
            _tmpExpiryDate = null
          } else {
            _tmpExpiryDate = _stmt.getLong(_columnIndexOfExpiryDate)
          }
          val _tmpDateAdded: Long
          _tmpDateAdded = _stmt.getLong(_columnIndexOfDateAdded)
          _result =
              PantryItem(_tmpId,_tmpName,_tmpBrand,_tmpCategory,_tmpQuantity,_tmpNotes,_tmpExpiryDate,_tmpDateAdded)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: String) {
    val _sql: String = "DELETE FROM pantry_items WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
