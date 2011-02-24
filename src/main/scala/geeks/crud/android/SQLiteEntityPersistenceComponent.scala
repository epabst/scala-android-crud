package geeks.crud.android

import android.provider.BaseColumns
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.database.Cursor
import java.lang.Byte
import android.content.{Context, ContentValues}
import geeks.crud.util.Logging
import geeks.crud._

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */
trait SQLiteEntityPersistenceComponent {
  def databaseSetup: SQLiteOpenHelper

  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase

  class SQLiteEntityPersistence(entityConfig: AndroidEntityCrudConfig) extends EntityPersistence[Long,Cursor,Cursor,ContentValues] with Logging {
    //may be overridden to affect findAll
    def selection: String = null
    //may be overridden to affect findAll
    def selectionArgs: Array[String] = Nil.toArray
    //may be overridden to affect findAll
    def groupBy: String = null
    //may be overridden to affect findAll
    def having: String = null
    //may be overridden to affect findAll
    def orderBy: String = null

    final lazy val queryFieldNames: List[String] = CursorFieldAccess.queryFieldNames(entityConfig.fields)

    def findAll: Cursor = database.query(entityConfig.entityName, queryFieldNames.toArray,
      selection, selectionArgs, groupBy, having, orderBy)

    def find(id: Long) = database.query(entityConfig.entityName, queryFieldNames.toArray,
      BaseColumns._ID + "=" + id, Nil.toArray, groupBy, having, orderBy)

    def newWritable = new ContentValues

    def save(idOption: Option[Long], contentValues: ContentValues): Long = {
      idOption match {
        case None => {
          info("Adding " + entityConfig.entityName + " with " + contentValues)
          database.insert(entityConfig.entityName, null, contentValues)
        }
        case Some(id) => {
          info("Updating " + entityConfig.entityName + " #" + id + " with " + contentValues)
          database.update(entityConfig.entityName, contentValues, BaseColumns._ID + "=" + id, null)
          id
        }
      }
    }

    def delete(ids: List[Long]) {
      ids.foreach(id => database.delete(entityConfig.entityName, BaseColumns._ID + "=" + id, Nil.toArray))
    }
  }
}