package com.github.scala.android.crud

import android.provider.BaseColumns
import android.database.Cursor
import android.content.ContentValues
import com.github.triangle.Logging
import persistence.{SQLiteCriteria, CursorField}
import scala.None
import collection.mutable.SynchronizedQueue
import android.app.backup.BackupManager
import android.database.sqlite.{SQLiteOpenHelper, SQLiteDatabase}
import action.UriPath

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */
class SQLiteEntityPersistence(val entityType: SQLiteCrudType, crudContext: CrudContext)
  extends CrudPersistence with Logging {

  lazy val databaseSetup = new GeneratedDatabaseSetup(crudContext)
  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase
  private lazy val backupManager = new BackupManager(crudContext.context)
  private var cursors = new SynchronizedQueue[Cursor]

  lazy val queryFieldNames: List[String] = CursorField.queryFieldNames(entityType)

  override lazy val logTag = classOf[CrudPersistence].getName +
          "(" + entityType.entityName + ")"

  def findAll(criteria: SQLiteCriteria): Cursor = {
    debug("Finding each " + entityType.entityName + " for " + queryFieldNames.mkString(",") + " where " + criteria.selection)
    val cursor = database.query(entityType.tableName, queryFieldNames.toArray,
      criteria.selection, criteria.selectionArgs, criteria.groupBy, criteria.having, criteria.orderBy)
    cursors += cursor
    cursor
  }

  //Unit is provided here in the item list for the sake of PortableField.adjustment[SQLiteCriteria] fields
  def findAll(uri: UriPath) = CursorStream(findAll(entityType.transformWithItem(new SQLiteCriteria, List(uri, Unit))))

  //todo delete?
  override def find(id: ID): Option[Cursor] = {
    debug("Finding " + entityType.entityName + " for " + queryFieldNames.mkString(",") + " where " + BaseColumns._ID + "=" + id)
    val cursor = database.query(entityType.tableName, queryFieldNames.toArray,
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
        database.insert(entityType.tableName, null, contentValues)
      }
      case Some(givenId) => {
        info("Updating " + entityType.entityName + " #" + givenId + " with " + contentValues)
        val rowCount = database.update(entityType.tableName, contentValues, BaseColumns._ID + "=" + givenId, null)
        if (rowCount == 0) {
          contentValues.put(BaseColumns._ID, givenId)
          info("Added " + entityType.entityName + " #" + givenId + " with " + contentValues + " since id is not present yet")
          val resultingId = database.insert(entityType.tableName, null, contentValues)
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
      database.delete(entityType.tableName, BaseColumns._ID + "=" + id, Nil.toArray)
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

case class CursorStream(cursor: Cursor) extends Stream[Cursor] {
  private val cursorIterator = new CalculatedIterator[Cursor] {
    def calculateNextValue() = if (cursor.moveToNext) Some(cursor) else {
      cursor.close()
      None
    }
  }

  override def isEmpty = cursorIterator.isEmpty

  override def head = cursorIterator.head

  override def tail = {
    cursorIterator.next()
    this
  }

  protected def tailDefined = cursor.getPosition < cursor.getCount - 1
}

class GeneratedDatabaseSetup(crudContext: CrudContext) extends SQLiteOpenHelper(crudContext.context, crudContext.application.nameId, null, 1) with Logging {

  def onCreate(db: SQLiteDatabase) {
    val application = crudContext.application
    for (val entityType <- application.allEntities.collect { case c: SQLiteCrudType => c }) {
      val buffer = new StringBuffer
      buffer.append("CREATE TABLE IF NOT EXISTS ").append(entityType.tableName).append(" (").
          append(BaseColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT")
      CursorField.persistedFields(entityType).filter(_.name != BaseColumns._ID).foreach { persisted =>
        buffer.append(", ").append(persisted.name).append(" ").append(persisted.persistedType.sqliteType)
      }
      buffer.append(")")
      execSQL(db, buffer.toString)
    }
  }

  private def execSQL(db: SQLiteDatabase, sql: String) {
    debug("execSQL: " + sql)
    db.execSQL(sql)
  }

  def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    // Steps to upgrade the database for the new version ...
    // This shouldn't be necessary here since a new database is created when
    // a new version of the application is installed.
  }
}
