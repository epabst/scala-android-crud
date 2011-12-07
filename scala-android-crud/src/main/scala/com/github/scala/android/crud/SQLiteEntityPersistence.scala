package com.github.scala.android.crud

import android.provider.BaseColumns
import android.database.Cursor
import android.content.ContentValues
import com.github.triangle.Logging
import common.Common
import common.PlatformTypes._
import persistence._
import scala.None
import collection.mutable.SynchronizedQueue
import android.app.backup.BackupManager
import android.database.sqlite.{SQLiteOpenHelper, SQLiteDatabase}
import common.UriPath

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */
class SQLiteEntityPersistence(val entityType: EntityType, val crudContext: CrudContext)
  extends CrudPersistence with Logging {

  lazy val tableName = SQLitePersistenceFactory.toTableName(entityType.entityName)
  lazy val databaseSetup = new GeneratedDatabaseSetup(crudContext)
  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase
  private lazy val backupManager = new BackupManager(crudContext.context)
  private var cursors = new SynchronizedQueue[Cursor]

  lazy val persistedFields: List[CursorField[_]] = CursorField.persistedFields(entityType)
  lazy val queryFieldNames: List[String] = persistedFields.map(_.columnName)

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  def findAll(criteria: SQLiteCriteria): CursorStream = {
    val query = criteria.selection.mkString(" AND ")
    info("Finding each " + entityType.entityName + "'s " + queryFieldNames.mkString(", ") + " where " + query)
    val cursor = database.query(tableName, queryFieldNames.toArray,
      toOption(query).getOrElse(null), criteria.selectionArgs.toArray,
      criteria.groupBy.getOrElse(null), criteria.having.getOrElse(null), criteria.orderBy.getOrElse(null))
    cursors += cursor
    CursorStream(cursor, persistedFields)
  }

  //Unit is provided here in the item list for the sake of PortableField.adjustment[SQLiteCriteria] fields
  def findAll(uri: UriPath) = findAll(entityType.transformWithItem(new SQLiteCriteria, List(uri, Unit)))

  private def notifyDataChanged() {
    backupManager.dataChanged()
    debug("Notified BackupManager that data changed.")
  }

  def newWritable = SQLitePersistenceFactory.newWritable

  protected def doSave(idOption: Option[ID], writable: AnyRef): ID = {
    val contentValues = writable.asInstanceOf[ContentValues]
    val id = idOption match {
      case None => {
        info("Adding " + entityType.entityName + " with " + contentValues)
        database.insert(tableName, null, contentValues)
      }
      case Some(givenId) => {
        info("Updating " + entityType.entityName + " #" + givenId + " with " + contentValues)
        val rowCount = database.update(tableName, contentValues, BaseColumns._ID + "=" + givenId, null)
        if (rowCount == 0) {
          contentValues.put(BaseColumns._ID, givenId)
          info("Added " + entityType.entityName + " #" + givenId + " with " + contentValues + " since id is not present yet")
          val resultingId = database.insert(tableName, null, contentValues)
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

  protected def doDelete(uri: UriPath) {
    val ids = findAll(uri).map { readable =>
      val id = entityType.IdField(readable)
      database.delete(tableName, BaseColumns._ID + "=" + id, Nil.toArray)
      id
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

class GeneratedDatabaseSetup(crudContext: CrudContext) extends SQLiteOpenHelper(crudContext.context, crudContext.application.nameId, null, 1) with Logging {
  protected lazy val logTag = Common.tryToEvaluate(crudContext.application.logTag).getOrElse(Common.logTag)

  def onCreate(db: SQLiteDatabase) {
    val application = crudContext.application
    for (entityType <- application.allEntities.collect { case c: PersistedCrudType => c }) {
      val buffer = new StringBuffer
      buffer.append("CREATE TABLE IF NOT EXISTS ").append(SQLitePersistenceFactory.toTableName(entityType.entityName)).append(" (").
          append(BaseColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT")
      CursorField.persistedFields(entityType).filter(_.columnName != BaseColumns._ID).foreach { persisted =>
        buffer.append(", ").append(persisted.columnName).append(" ").append(persisted.persistedType.sqliteType)
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
