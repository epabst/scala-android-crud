package com.github.scala_android.crud

import _root_.android.app.Activity
import _root_.android.widget.ResourceCursorAdapter
import android.provider.BaseColumns
import android.view.View
import android.database.sqlite.SQLiteDatabase
import android.database.Cursor
import android.content.{Context, ContentValues}
import com.github.scala_android.crud.monitor.Logging

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */
class SQLiteEntityPersistence(entityConfig: SQLiteCrudEntityConfig, context: Context)
  extends EntityPersistence[SQLiteCriteria,Cursor,Cursor,ContentValues] with Logging {

  lazy val databaseSetup = entityConfig.getDatabaseSetup(context)
  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase

  lazy val queryFieldNames: List[String] = CursorFieldAccess.queryFieldNames(entityConfig.fields)

  override lazy val logTag = classOf[EntityPersistence[SQLiteCriteria,Cursor,Cursor,ContentValues]].getName +
          "(" + entityConfig.entityName + ")"

  def newCriteria = new SQLiteCriteria

  def findAll(criteria: SQLiteCriteria): Cursor = {
    debug("Querying " + entityConfig.entityName + " for " + queryFieldNames.mkString(",") + " where " + criteria.selection)
    database.query(entityConfig.entityName, queryFieldNames.toArray,
      criteria.selection, criteria.selectionArgs, criteria.groupBy, criteria.having, criteria.orderBy)
  }

  def createListAdapter(activity: Activity): ResourceCursorAdapter = {
    val criteria = newCriteria
    entityConfig.copyFields(activity.getIntent, criteria)
    val cursor = findAll(criteria)
    activity.startManagingCursor(cursor)
    new ResourceCursorAdapter(activity, entityConfig.rowLayout, cursor) {
      def bindView(view: View, context: Context, cursor: Cursor) {
        entityConfig.copyFields(cursor, view)
      }
    }
  }

  //todo deal with not finding any match by returning an Option[R]
  def find(id: ID) = {
    val cursor = database.query(entityConfig.entityName, queryFieldNames.toArray,
      BaseColumns._ID + "=" + id, Nil.toArray, null, null, null)
    cursor.moveToFirst
    cursor
  }

  def newWritable = new ContentValues

  def save(idOption: Option[ID], contentValues: ContentValues): ID = {
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

  def delete(ids: List[ID]) {
    ids.foreach(id => database.delete(entityConfig.entityName, BaseColumns._ID + "=" + id, Nil.toArray))
  }

  def close() {
    database.close()
  }
}

class SQLiteCriteria(var selection: String = null, var selectionArgs: Array[String] = Nil.toArray,
                     var groupBy: String = null, var having: String = null, var orderBy: String = null)
