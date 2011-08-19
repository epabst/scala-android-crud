package com.github.scala_android.crud

import android.content.{ContentValues, Context}
import android.view.View
import android.app.ListActivity
import android.database.Cursor
import android.widget.{CursorAdapter, ListAdapter, ResourceCursorAdapter}

/**
 * A CrudType for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */

trait SQLiteCrudType extends CrudType {
  def newWritable = new ContentValues

  def openEntityPersistence(crudContext: CrudContext) = new SQLiteEntityPersistence(this, crudContext)

  def setListAdapter(findAllResult: AnyRef, contextItems: List[AnyRef], activity: ListActivity) {
    val cursor = findAllResult.asInstanceOf[Cursor]
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
