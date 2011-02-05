package geeks.crud.android

import android.provider.BaseColumns
import geeks.financial.futurebalance.persistence.EntityPersistence
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.database.Cursor
import android.view.View
import android.content.ContentValues
import java.lang.Byte

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */


abstract class SQLiteEntityPersistence[T] extends EntityPersistence[T] {
  val idField = new SimpleSQLiteField[T](BaseColumns._ID) {
    override val viewResourceIds = List.empty[Int]
    val persistedFieldNamesWithView = List.empty[String]

    def valuesToPersist(entity: T): Map[String, Object] = Map.empty

    def copyToEntity(fromEntryView: View, toEntity: T) {}

    def copyToView(fromEntity: T, toEntryView: View) {}
  }

  val entityName: String
  val fields: List[Field[T]]

  def databaseSetup: SQLiteOpenHelper
  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase

  //may be overridden
  def selection: String = null
  //may be overridden
  def selectionArgs: Array[String] = Nil.toArray
  //may be overridden
  def groupBy: String = null
  //may be overridden
  def having: String = null
  //may be overridden
  def orderBy: String = null

  def data: Cursor = database.query(entityName, fields.flatMap(_.queryFieldNames).toArray, selection, selectionArgs, groupBy, having, orderBy)

  private def toContentValues(values: Map[String, Any]): ContentValues = {
    val contentValues = new ContentValues()
    for ((key, value) <- values) value match {
      case v: Object if (v == null) => contentValues.putNull(key)
      case v: String => contentValues.put(key, v)
      case v: Byte => JavaHelper.putByte(contentValues, key, v)
      case v: Short => JavaHelper.putShort(contentValues, key, v)
      case v: Int => JavaHelper.putInt(contentValues, key, v)
      case v: Long => JavaHelper.putLong(contentValues, key, v)
      case v: Float => JavaHelper.putFloat(contentValues, key, v)
      case v: Double => JavaHelper.putDouble(contentValues, key, v)
      case v: Boolean => contentValues.put(key, v)
      case v: Array[Byte] => JavaHelper.putByteArray(contentValues, key, v)
      case v => throw new IllegalStateException("Unsupported type for ContentValues: " + v)
    }
    contentValues
  }

  //override during unit testing
  def insertIntoDatabase(values: Map[String, Any]) {
    database.insert(entityName, null, toContentValues(values))
  }

  def save(entity: T) {
    val values: Map[String, Object] = Map.empty ++ fields.flatMap(_.valuesToPersist(entity))
    insertIntoDatabase(values)
  }
}

abstract class SimpleSQLiteField[E](val name: String) extends Field[E] {
  val queryFieldNames = List(name)

}

