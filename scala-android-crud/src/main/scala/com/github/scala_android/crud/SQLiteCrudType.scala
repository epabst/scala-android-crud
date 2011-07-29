package com.github.scala_android.crud

import android.content.{ContentValues, Context}
import android.widget.ResourceCursorAdapter
import android.view.View
import monitor.Logging
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.provider.BaseColumns
import com.github.triangle.JavaUtil.toRunnable
import android.app.ListActivity
import com.github.triangle.PortableValue
import android.database.{ContentObserver, Cursor}

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

    override def delete(ids: List[ID]) {
      super.delete(ids)
      debug("requerying after delete")
      cursorVarForListAdapter.get(crudContext).map(_.requery())
    }
  }

  val cursorVarForListAdapter = new ContextVar[Cursor]

  def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: ListActivity) {
    setListAdapter(persistence.asInstanceOf[SQLiteEntityPersistence], crudContext, activity)
  }

  private def findPortableValue(activity: ListActivity, position: Long): Option[PortableValue] =
    Option[Map[Long,PortableValue]](activity.getListView.getTag.asInstanceOf[Map[Long,PortableValue]]).flatMap(_.get(position))

  private def setPortableValue(activity: ListActivity, position: Long, portableValue: PortableValue) {
    val listView = activity.getListView
    val map = Option[Map[Long,PortableValue]](listView.getTag.asInstanceOf[Map[Long,PortableValue]]).getOrElse(Map.empty[Long,PortableValue]) +
            (position -> portableValue)
    listView.setTag(map)
  }

  private def clearPortableValues(activity: ListActivity) {
    activity.getListView.setTag(null)
  }

  def setListAdapter(persistence: SQLiteEntityPersistence, crudContext: CrudContext, activity: ListActivity) {
    val intent = activity.getIntent
    val contextItems = List(intent, crudContext, Unit)
    val criteria = persistence.newCriteria
    copy(intent, criteria)
    val cursor = persistence.findAll(criteria)
    cursorVarForListAdapter.set(crudContext, cursor)
    activity.startManagingCursor(cursor)
    val observer = new ContentObserver(null) {
      override def onChange(selfChange: Boolean) {
        activity.runOnUiThread { clearPortableValues(activity) }
        super.onChange(selfChange)
      }
    }
    cursor.registerContentObserver(observer)
    activity.setListAdapter(new ResourceCursorAdapter(activity, rowLayout, cursor) {
      def bindView(view: View, context: Context, cursor: Cursor) {
        val position = cursor.getPosition
        //set the cached or default values immediately instead of showing the column header names
        val cachedValue = findPortableValue(activity, position)
        cachedValue.getOrElse(copyFrom(Unit)).copyTo(view)
        if (cachedValue.isEmpty) {
          //copy from the cursor immediately since it will be advanced to the next row quickly.
          val cursorValues = transform(Map[String,Any](), cursor)
          future {
            val portableValue = copyFromItem(cursorValues :: contextItems)
            activity.runOnUiThread {
              setPortableValue(activity, position, portableValue)
              notifyDataSetChanged()
            }
          }
        }
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
