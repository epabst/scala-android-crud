package geeks.crud.android

import android.provider.BaseColumns
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.database.Cursor
import java.lang.Byte
import geeks.crud.EntityPersistenceComponent
import android.content.{Context, ContentValues}
import geeks.crud.util.Logging
import geeks.crud._

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */
trait SQLiteEntityPersistenceComponent extends EntityPersistenceComponent[Cursor,Cursor,ContentValues] {
  type ID = Long

  def entityName: String

  def fields: List[CopyableField]

  def context: Context

  def databaseSetup: SQLiteOpenHelper

  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase

  lazy val persistence: SQLiteEntityPersistence = new SQLiteEntityPersistence

  class SQLiteEntityPersistence extends EntityPersistence with Logging {
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

    final lazy val queryFieldNames: List[String] = CursorFieldAccess.queryFieldNames(fields)

    def findAll: Cursor = database.query(entityName, queryFieldNames.toArray,
      selection, selectionArgs, groupBy, having, orderBy)

    def find(id: ID) = database.query(entityName, queryFieldNames.toArray,
      BaseColumns._ID + "=" + id, Nil.toArray, groupBy, having, orderBy)

    def newWritable = new ContentValues

    def save(idOption: Option[ID], contentValues: ContentValues): ID = {
      idOption match {
        case None => {
          info("Adding " + entityName + " with " + contentValues)
          database.insert(entityName, null, contentValues)
        }
        case Some(id) => {
          info("Updating " + entityName + " #" + id + " with " + contentValues)
          database.update(entityName, contentValues, BaseColumns._ID + "=" + id, null)
          id
        }
      }
    }

    def delete(ids: List[ID]) {
      ids.foreach(id => database.delete(entityName, BaseColumns._ID + "=" + id, Nil.toArray))
    }
  }
}