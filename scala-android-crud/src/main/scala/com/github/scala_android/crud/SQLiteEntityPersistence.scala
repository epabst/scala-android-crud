package com.github.scala_android.crud

import android.provider.BaseColumns
import android.database.sqlite.SQLiteDatabase
import android.database.Cursor
import android.content.ContentValues
import com.github.scala_android.crud.common.Logging
import persistence.{SQLiteCriteria, CursorField, CrudPersistence}
import scala.None
import collection.mutable.SynchronizedQueue
import android.app.backup.BackupManager

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */
class SQLiteEntityPersistence(val entityType: SQLiteCrudType, crudContext: CrudContext)
  extends CrudPersistence with Logging {

  lazy val databaseSetup = entityType.getDatabaseSetup(crudContext)
  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase
  private lazy val backupManager = new BackupManager(crudContext.context)
  private var cursors = new SynchronizedQueue[Cursor]

  lazy val queryFieldNames: List[String] = CursorField.queryFieldNames(entityType)

  override lazy val logTag = classOf[CrudPersistence].getName +
          "(" + entityType.entityName + ")"

  def newCriteria: SQLiteCriteria = new SQLiteCriteria

  def findAll(criteria: AnyRef): Cursor = findAll(criteria.asInstanceOf[SQLiteCriteria])

  def findAll(criteria: SQLiteCriteria): Cursor = {
    debug("Finding each " + entityType.entityName + " for " + queryFieldNames.mkString(",") + " where " + criteria.selection)
    val cursor = database.query(entityType.entityName, queryFieldNames.toArray,
      criteria.selection, criteria.selectionArgs, criteria.groupBy, criteria.having, criteria.orderBy)
    cursors += cursor
    cursor
  }

  def toIterator(list: AnyRef): Iterator[Cursor] = toIterator(list.asInstanceOf[Cursor])

  def toIterator(list: Cursor) = new CalculatedIterator[Cursor] {
    def calculateNextValue() = if (list.moveToNext) Some(list) else {
      list.close()
      None
    }
  }

  def find(id: ID): Option[Cursor] = {
    debug("Finding " + entityType.entityName + " for " + queryFieldNames.mkString(",") + " where " + BaseColumns._ID + "=" + id)
    val cursor = database.query(entityType.entityName, queryFieldNames.toArray,
      BaseColumns._ID + "=" + id, Nil.toArray, null, null, null)
    if (cursor.moveToFirst) {
      cursors += cursor
      Some(cursor)
    } else {
      cursor.close()
      None
    }
  }

  private def notifyDataChanged() {
    backupManager.dataChanged()
    debug("Notified BackupManager that data changed.")
  }

  protected def doSave(idOption: Option[ID], writable: AnyRef): ID = {
    val contentValues = writable.asInstanceOf[ContentValues]
    val id = idOption match {
      case None => {
        info("Adding " + entityType.entityName + " with " + contentValues)
        database.insert(entityType.entityName, null, contentValues)
      }
      case Some(givenId) => {
        info("Updating " + entityType.entityName + " #" + givenId + " with " + contentValues)
        val rowCount = database.update(entityType.entityName, contentValues, BaseColumns._ID + "=" + givenId, null)
        if (rowCount == 0) {
          contentValues.put(BaseColumns._ID, givenId)
          info("Added " + entityType.entityName + " #" + givenId + " with " + contentValues + " since id is not present yet")
          val resultingId = database.insert(entityType.entityName, null, contentValues)
          if (givenId != resultingId)
            throw new IllegalStateException("id changed from " + givenId + " to " + resultingId +
                    " when restoring " + entityType.entityName + " #" + givenId + " with " + contentValues)
        }
        givenId
      }
    }
    notifyDataChanged()
    val map = entityType.transform(Map[String,Any](), contentValues)
    val bytes = CrudBackupAgent.marshall(map)
    debug("Scheduled backup which will include " + entityType.entityName + "#" + id + ": size " + bytes.size + " bytes")
    id
  }

  protected def doDelete(ids: Seq[ID]) {
    ids.foreach { id =>
      database.delete(entityType.entityName, BaseColumns._ID + "=" + id, Nil.toArray)
    }
    future {
      ids.foreach { id =>
        DeletedEntityIdCrudType.recordDeletion(entityType, id, crudContext.context)
      }
      notifyDataChanged()
    }
  }

  def close() {
    cursors.map(_.close())
    database.close()
  }
}
