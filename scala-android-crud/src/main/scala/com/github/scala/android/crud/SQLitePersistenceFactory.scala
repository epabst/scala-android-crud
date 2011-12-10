package com.github.scala.android.crud

import android.content.{ContentValues, Context}
import android.view.View
import android.database.Cursor
import android.widget.{CursorAdapter, ListAdapter, ResourceCursorAdapter}
import persistence.{SQLiteUtil, EntityType, CursorStream}

/**
 * A PersistenceFactory for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */
object SQLitePersistenceFactory extends PersistenceFactory {
  def newWritable = new ContentValues

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = new SQLiteEntityPersistence(entityType, crudContext)

  def setListAdapter(crudType: CrudType, findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {
    val CursorStream(cursor, _) = findAllResult
    activity.startManagingCursor(cursor)
    activity.setListAdapter(new ResourceCursorAdapter(activity, crudType.rowLayout, cursor) with AdapterCaching {
      def entityType = crudType.entityType

      def bindView(view: View, context: Context, cursor: Cursor) {
        bindViewFromCacheOrItems(view, entityType.transform(Map[String,Any](), cursor) :: contextItems, cursor.getPosition, activity)
      }
    })
  }

  def refreshAfterDataChanged(listAdapter: ListAdapter) {
    listAdapter match {
      case cursorAdapter: CursorAdapter => cursorAdapter.getCursor.requery()
    }
  }

  def toTableName(entityName: String): String = SQLiteUtil.toNonReservedWord(entityName)
}
