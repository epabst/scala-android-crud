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
import actors.Future

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

  private def findFuturePortableValue(activity: ListActivity, position: Long): Option[Future[PortableValue]] =
    Option(activity.getListView.getTag.asInstanceOf[Map[Long, Future[PortableValue]]]).flatMap(_.get(position))

  private def findReadyValue[T](future: Future[T]): Option[T] = if (future.isSet) Some(future()) else None

  private def setFuturePortableValue(activity: ListActivity, position: Long, futurePortableValue: Future[PortableValue]) {
    val listView = activity.getListView
    val map = Option(listView.getTag.asInstanceOf[Map[Long,Future[PortableValue]]]).getOrElse(Map.empty[Long,Future[PortableValue]]) +
            (position -> futurePortableValue)
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
        val position: Long = cursor.getPosition
        val futureValue: Option[Future[PortableValue]] = findFuturePortableValue(activity, position)
        //set the cached or default values immediately instead of showing the column header names
        futureValue.flatMap(findReadyValue(_)).getOrElse(copyFrom(Unit)).copyTo(view)
        if (futureValue.isEmpty) {
          //copy from the cursor immediately since it will be advanced to the next row quickly.
          val cursorValues = transform(Map[String,Any](), cursor)
          setFuturePortableValue(activity, position, future {
            val portableValue = copyFromItem(cursorValues :: contextItems)
            activity.runOnUiThread { notifyDataSetChanged() }
            portableValue
          })
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
