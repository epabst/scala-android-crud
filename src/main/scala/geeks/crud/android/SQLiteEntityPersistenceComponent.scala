package geeks.crud.android

import android.provider.BaseColumns
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.database.Cursor
import java.lang.Byte
import geeks.crud.persistence.IdPk
import geeks.crud.EntityPersistenceComponent
import android.content.{Context, ContentValues}

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */
trait SQLiteEntityPersistenceComponent[T <: IdPk] extends EntityPersistenceComponent[T] {
  def context: Context

  def databaseSetup: SQLiteOpenHelper

  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase

  def persistence: SQLiteEntityPersistence[T]

  abstract class SQLiteEntityPersistence[T <: IdPk] extends EntityPersistence[T] {
    val entityName: String
    val fields: List[Field[T]]

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

    //override during unit testing
    def data: Cursor = database.query(entityName, (BaseColumns._ID :: fields.flatMap(_.queryFieldNames)).toArray,
      selection, selectionArgs, groupBy, having, orderBy)

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
      val values: Map[String, Any] = Map.empty ++ fields.flatMap(_.valuesToPersist(entity))
      insertIntoDatabase(values)
    }
  }
}