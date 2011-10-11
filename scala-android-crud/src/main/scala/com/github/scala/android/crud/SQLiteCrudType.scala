package com.github.scala.android.crud

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

  lazy val tableName = toNonReservedWord(entityName)

  def openEntityPersistence(crudContext: CrudContext) = new SQLiteEntityPersistence(this, crudContext)

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

  def toNonReservedWord(name: String): String =  name.toUpperCase match {
    case "ABORT" | "ACTION" | "ADD" | "AFTER" | "ALL" | "ALTER" | "ANALYZE" | "AND" | "AS" | "ASC" | "ATTACH" |
      "AUTOINCREMENT" | "BEFORE" | "BEGIN" | "BETWEEN" | "BY" | "CASCADE" | "CASE" | "CAST" | "CHECK" |
      "COLLATE" | "COLUMN" | "COMMIT" | "CONFLICT" | "CONSTRAINT" | "CREATE" | "CROSS" | "CURRENT_DATE" |
      "CURRENT_TIME" | "CURRENT_TIMESTAMP" | "DATABASE" | "DEFAULT" | "DEFERRABLE" | "DEFERRED" | "DELETE" |
      "DESC" | "DETACH" | "DISTINCT" | "DROP" | "EACH" | "ELSE" | "END" | "ESCAPE" | "EXCEPT" | "EXCLUSIVE" |
      "EXISTS" | "EXPLAIN" | "FAIL" | "FOR" | "FOREIGN" | "FROM" | "FULL" | "GLOB" | "GROUP" | "HAVING" |
      "IF" | "IGNORE" | "IMMEDIATE" | "IN" | "INDEX" | "INDEXED" | "INITIALLY" | "INNER" | "INSERT" | "INSTEAD" |
      "INTERSECT" | "INTO" | "IS" | "ISNULL" | "JOIN" | "KEY" | "LEFT" | "LIKE" | "LIMIT" | "MATCH" | "NATURAL" |
      "NO" | "NOT" | "NOTNULL" | "NULL" | "OF" | "OFFSET" | "ON" | "OR" | "ORDER" | "OUTER" | "PLAN" |
      "PRAGMA" | "PRIMARY" | "QUERY" | "RAISE" | "REFERENCES" | "REGEXP" | "REINDEX" | "RELEASE" | "RENAME" |
      "REPLACE" | "RESTRICT" | "RIGHT" | "ROLLBACK" | "ROW" | "SAVEPOINT" | "SELECT" | "SET" | "TABLE" |
      "TEMP" | "TEMPORARY" | "THEN" | "TO" | "TRANSACTION" | "TRIGGER" | "UNION" | "UNIQUE" | "UPDATE" |
      "USING" | "VACUUM" | "VALUES" | "VIEW" | "VIRTUAL" | "WHEN" | "WHERE" => name + "0"
    case _ => name
  }
}
