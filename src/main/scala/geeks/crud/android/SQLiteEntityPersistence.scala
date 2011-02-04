package geeks.crud.android

import android.provider.BaseColumns
import geeks.financial.futurebalance.persistence.EntityPersistence
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.database.Cursor
import android.content.ContentValues
import android.view.View

/**
 * EntityPersistence for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 6:17 PM
 */


abstract class SQLiteEntityPersistence[T] extends EntityPersistence[T] {
  val idField = new SimpleSQLiteField[T](BaseColumns._ID) {
    override val viewResourceIds = List.empty[Int]
    val persistedFieldNamesWithView = List.empty[String]

    def copyToContent(entity: T, values: ContentValues) {}

    def copyToEntity(fromEntryView: View, toEntity: T) {}

    def copyToView(fromEntity: T, toEntryView: View) {}
  }

  val entityName: String
  val fields: List[Field[T]]

  def databaseSetup: SQLiteOpenHelper
  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase

  //may be overridden
  def selection: String = null
  //may be overridden
  def selectionArgs: Array[String] = Nil.toArray
  //may be overridden
  def groupBy: String = null
  //may be overridden
  def having: String = null
  //may be overridden
  def orderBy: String = null

  def data: Cursor = database.query(entityName, fields.flatMap(_.queryFieldNames).toArray, selection, selectionArgs, groupBy, having, orderBy)

  def save(entity: T) {
    val values: ContentValues = new ContentValues
    fields.foreach(_.copyToContent(entity, values))
    database.insert(entityName, null, values)
  }
}

abstract class SimpleSQLiteField[E](val name: String) extends Field[E] {
  val queryFieldNames = List(name)

}

