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
class SQLiteEntityPersistence(entityConfig: SQLiteCrudEntityConfig, context: Context) extends EntityPersistence[Long,SQLiteCriteria,Cursor,Cursor,ContentValues] with Logging {
  lazy val databaseSetup = entityConfig.getDatabaseSetup(context)
  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase

  lazy val queryFieldNames: List[String] = CursorFieldAccess.queryFieldNames(entityConfig.fields)

  def newCriteria = new SQLiteCriteria

  def findAll(criteria: SQLiteCriteria): Cursor = database.query(entityConfig.entityName, queryFieldNames.toArray,
    criteria.selection, criteria.selectionArgs, criteria.groupBy, criteria.having, criteria.orderBy)

  def find(id: Long) = database.query(entityConfig.entityName, queryFieldNames.toArray,
    BaseColumns._ID + "=" + id, Nil.toArray, null, null, null)

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

class SQLiteCriteria(var selection: String = null, var selectionArgs: Array[String] = Nil.toArray,
                     var groupBy: String = null, var having: String = null, var orderBy: String = null)
