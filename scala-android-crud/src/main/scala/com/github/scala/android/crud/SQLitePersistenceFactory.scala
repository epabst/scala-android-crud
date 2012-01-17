package com.github.scala.android.crud

import android.content.ContentValues
import persistence.{SQLiteUtil, EntityType}

/** A PersistenceFactory for SQLite.
  * @author Eric Pabst (epabst@gmail.com)
  */
object SQLitePersistenceFactory extends PersistenceFactory {
  def newWritable = new ContentValues

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext) = new SQLiteEntityPersistence(entityType, crudContext)

  def toTableName(entityName: String): String = SQLiteUtil.toNonReservedWord(entityName)
}
