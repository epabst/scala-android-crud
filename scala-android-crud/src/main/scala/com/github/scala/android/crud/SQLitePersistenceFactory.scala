package com.github.scala.android.crud

import android.content.ContentValues
import android.widget.{CursorAdapter, ListAdapter}
import persistence.{SQLiteUtil, EntityType}

/** A PersistenceFactory for SQLite.
  * @author Eric Pabst (epabst@gmail.com)
  */
object SQLitePersistenceFactory extends PersistenceFactory {
  def newWritable = new ContentValues

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = new SQLiteEntityPersistence(entityType, crudContext)

  def refreshAfterDataChanged(listAdapter: ListAdapter) {
    listAdapter match {
      case cursorAdapter: CursorAdapter => cursorAdapter.getCursor.requery()
      case _ =>
    }
  }

  def toTableName(entityName: String): String = SQLiteUtil.toNonReservedWord(entityName)
}
