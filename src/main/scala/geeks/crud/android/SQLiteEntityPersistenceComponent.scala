package geeks.crud.android

import android.provider.BaseColumns
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.database.Cursor
import java.lang.Byte
import geeks.crud.EntityPersistenceComponent
import android.content.{Context, ContentValues}

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */
trait SQLiteEntityPersistenceComponent[T] extends EntityPersistenceComponent[T] {
  def entityName: String

  def fields: List[Field[T]]

  def context: Context

  def databaseSetup: SQLiteOpenHelper

  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase

  def persistence: SQLiteEntityPersistence[T]

  abstract class SQLiteEntityPersistence[T] extends EntityPersistence[T] {
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
      for ((key, value) <- values) value.asInstanceOf[AnyRef] match {
        case v: Object if (v == null) => contentValues.putNull(key)
        case v: String => contentValues.put(key, v)
        case v: java.lang.Byte => JavaHelper.putByte(contentValues, key, v)
        case v: java.lang.Short => JavaHelper.putShort(contentValues, key, v.shortValue)
        case v: java.lang.Integer => JavaHelper.putInt(contentValues, key, v.intValue)
        case v: java.lang.Long => JavaHelper.putLong(contentValues, key, v.longValue)
        case v: java.lang.Float => JavaHelper.putFloat(contentValues, key, v.floatValue)
        case v: java.lang.Double => JavaHelper.putDouble(contentValues, key, v.doubleValue)
        case v: java.lang.Boolean => contentValues.put(key, v.booleanValue)
        case v: Array[Byte] => JavaHelper.putByteArray(contentValues, key, v)
        case v => throw new IllegalStateException("Unsupported type for ContentValues: " + v + " of type " + v.getClass)
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