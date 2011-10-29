package com.github.scala.android.crud

import android.content.{ContentValues, Context}
import android.view.View
import android.app.ListActivity
import android.database.Cursor
import android.widget.{CursorAdapter, ListAdapter, ResourceCursorAdapter}
import persistence.SQLiteUtil

/**
 * A CrudType for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */

trait SQLiteCrudType extends CrudType {
  def newWritable = new ContentValues

  lazy val tableName = SQLiteUtil.toNonReservedWord(entityName)

  protected def createEntityPersistence(crudContext: CrudContext) = new SQLiteEntityPersistence(this, crudContext)

  def setListAdapter(findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: ListActivity) {
    val CursorStream(cursor) = findAllResult
    activity.startManagingCursor(cursor)
    activity.setListAdapter(new ResourceCursorAdapter(activity, rowLayout, cursor) with AdapterCaching {
      cursor.registerDataSetObserver(cacheClearingObserver(activity))

      def bindView(view: View, context: Context, cursor: Cursor) {
        bindViewFromCacheOrItems(view, transform(Map[String,Any](), cursor) :: contextItems, cursor.getPosition, activity)
      }
    })
  }

  def refreshAfterDataChanged(listAdapter: ListAdapter) {
    listAdapter match {
      case cursorAdapter: CursorAdapter => cursorAdapter.getCursor.requery()
    }
  }
}
