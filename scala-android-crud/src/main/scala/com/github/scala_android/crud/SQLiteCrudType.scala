package com.github.scala_android.crud

import android.content.{ContentValues, Context}
import android.widget.ResourceCursorAdapter
import android.view.View
import monitor.Logging
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.provider.BaseColumns
import android.app.ListActivity
import android.database.Cursor

/**
 * A CrudType for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */

trait SQLiteCrudType extends CrudType {
  def newWritable = new ContentValues

  def openEntityPersistence(crudContext: CrudContext) = new SQLiteEntityPersistence(this, crudContext) {
    override def save(idOption: Option[ID], contentValues: ContentValues): ID = {
      val id = super.save(idOption, contentValues)
      debug("requerying after " + (if (idOption.isDefined) "updating" else "adding"))
      cursorVarForListAdapter.get(crudContext).map(_.requery())
      id
    }

    override def delete(ids: Seq[ID]) {
      super.delete(ids)
      debug("requerying after delete")
      cursorVarForListAdapter.get(crudContext).map(_.requery())
    }
  }

  val cursorVarForListAdapter = new ContextVar[Cursor]

  def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: ListActivity) {
    setListAdapter(persistence.asInstanceOf[SQLiteEntityPersistence], crudContext, activity)
  }

  def setListAdapter(persistence: SQLiteEntityPersistence, crudContext: CrudContext, activity: ListActivity) {
    val intent = activity.getIntent
    val contextItems = List(intent, crudContext, Unit)
    val cursor = persistence.findAll(transform(persistence.newCriteria, intent))
    cursorVarForListAdapter.set(crudContext, cursor)
    activity.startManagingCursor(cursor)
    activity.setListAdapter(new ResourceCursorAdapter(activity, rowLayout, cursor) with AdapterCaching {
      cursor.registerContentObserver(cacheClearingObserver(activity))

      def bindView(view: View, context: Context, cursor: Cursor) {
        bindViewFromCacheOrItems(view, transform(Map[String,Any](), cursor) :: contextItems, cursor.getPosition, activity)
      }
    })
  }

  def refreshAfterSave(crudContext: CrudContext) {
    cursorVarForListAdapter.get(crudContext).map(_.requery)
  }

  def getDatabaseSetup(crudContext: CrudContext): SQLiteOpenHelper = new GeneratedDatabaseSetup(crudContext)

  override def destroyContextVars(crudContext: CrudContext) {
    cursorVarForListAdapter.clear(crudContext).map(_.close())
    super.destroyContextVars(crudContext)
  }
}

class GeneratedDatabaseSetup(crudContext: CrudContext) extends SQLiteOpenHelper(crudContext.context, crudContext.application.nameId, null, 1) with Logging {

  def onCreate(db: SQLiteDatabase) {
    val application = crudContext.application
    for (val entityType <- application.allEntities) {
      val buffer = new StringBuffer
      buffer.append("CREATE TABLE IF NOT EXISTS ").append(entityType.entityName).append(" (").
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
