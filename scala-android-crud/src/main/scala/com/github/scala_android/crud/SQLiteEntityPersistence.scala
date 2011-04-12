package com.github.scala_android.crud

import android.provider.BaseColumns
import android.database.sqlite.SQLiteDatabase
import android.database.Cursor
import android.content.ContentValues
import com.github.scala_android.crud.monitor.Logging
import scala.None
import collection.mutable.{SynchronizedQueue, ListBuffer}
import android.app.backup.BackupManager

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */
class SQLiteEntityPersistence(entityType: SQLiteCrudEntityType, crudContext: CrudContext)
  extends EntityPersistence[SQLiteCriteria,Cursor,Cursor,ContentValues] with Logging {

  lazy val databaseSetup = entityType.getDatabaseSetup(crudContext.context)
  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase
  private lazy val backupManager = new BackupManager(crudContext.context)
  private var cursors = new SynchronizedQueue[Cursor]

  lazy val queryFieldNames: List[String] = CursorFieldAccess.queryFieldNames(entityType.fields)

  override lazy val logTag = classOf[EntityPersistence[SQLiteCriteria,Cursor,Cursor,ContentValues]].getName +
          "(" + entityType.entityName + ")"

  def newCriteria = new SQLiteCriteria

  def findAll(criteria: SQLiteCriteria): Cursor = {
    debug("Querying " + entityType.entityName + " for " + queryFieldNames.mkString(",") + " where " + criteria.selection)
    val cursor = database.query(entityType.entityName, queryFieldNames.toArray,
      criteria.selection, criteria.selectionArgs, criteria.groupBy, criteria.having, criteria.orderBy)
    cursors += cursor
    cursor
  }

  def toIterator(list: Cursor) = new CalculatedIterator[Cursor] {
    def calculateNextValue() = if (list.moveToNext) Some(list) else {
      list.close()
      None
    }
  }

  def findAll[T <: AnyRef](criteria: SQLiteCriteria, instantiate: () => T): List[T] = {
    val cursor = findAll(criteria)
    cursor.moveToFirst();
    var results = new ListBuffer[T]()
    while (cursor.isAfterLast == false) {
      val result = instantiate()
      entityType.copyFields(cursor, result)
      results += result
      cursor.moveToNext();
    }
    //consider treating this as an Iterator instead of a List, and closing the cursor at the end of the iterator's loop
    cursor.close();
    results.result()
  }

  def find(id: ID): Option[Cursor] = {
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

  def save(idOption: Option[ID], contentValues: ContentValues): ID = {
    val id = idOption match {
      case None => {
        info("Adding " + entityType.entityName + " with " + contentValues)
        database.insert(entityType.entityName, null, contentValues)
      }
      case Some(id) => {
        info("Updating " + entityType.entityName + " #" + id + " with " + contentValues)
        database.update(entityType.entityName, contentValues, BaseColumns._ID + "=" + id, null)
        id
      }
    }
    backupManager.dataChanged()
    id
  }

  def delete(ids: List[ID]) {
    ids.foreach(id => database.delete(entityType.entityName, BaseColumns._ID + "=" + id, Nil.toArray))
    backupManager.dataChanged()
  }

  def close() {
    cursors.map(_.close())
    database.close()
  }
}

class SQLiteCriteria(var selection: String = null, var selectionArgs: Array[String] = Nil.toArray,
                     var groupBy: String = null, var having: String = null, var orderBy: String = null)
