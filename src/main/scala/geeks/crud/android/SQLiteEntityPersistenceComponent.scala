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
trait SQLiteEntityPersistenceComponent extends EntityPersistenceComponent[Cursor,Cursor,ContentValues] {
  type ID = Long

  def entityName: String

  def fields: List[Field[Cursor,ContentValues]]

  def context: Context

  def databaseSetup: SQLiteOpenHelper

  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase

  lazy val persistence: SQLiteEntityPersistence = new SQLiteEntityPersistence

  class SQLiteEntityPersistence extends EntityPersistence {
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

    final lazy val queryFieldNames: List[String] = BaseColumns._ID :: fields.flatMap(_.queryFieldNames)

    def findAll: Cursor = database.query(entityName, queryFieldNames.toArray,
      selection, selectionArgs, groupBy, having, orderBy)

    def find(id: ID) = throw new UnsupportedOperationException("not implemented yet")

    def newWritable = new ContentValues

    def save(idOption: Option[ID], contentValues: ContentValues): ID = {
      idOption match {
        case None => database.insert(entityName, null, contentValues)
        case Some(id) => {
          database.update(entityName, contentValues, BaseColumns._ID + "=" + id, null)
          id
        }
      }
    }
  }
}