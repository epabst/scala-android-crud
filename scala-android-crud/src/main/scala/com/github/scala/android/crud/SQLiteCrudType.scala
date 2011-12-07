package com.github.scala.android.crud

import android.content.{ContentValues, Context}
import android.view.View
import android.database.Cursor
import android.widget.{CursorAdapter, ListAdapter, ResourceCursorAdapter}
import persistence.CursorStream

/**
 * A CrudType for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */

trait SQLiteCrudType extends PersistedCrudType {
  def newWritable = new ContentValues

  protected def createEntityPersistence(crudContext: CrudContext) = new SQLiteEntityPersistence(this, crudContext)

  def setListAdapter(findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {
    val CursorStream(cursor, _) = findAllResult
    activity.startManagingCursor(cursor)
    activity.setListAdapter(new ResourceCursorAdapter(activity, rowLayout, cursor) with AdapterCaching {
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
