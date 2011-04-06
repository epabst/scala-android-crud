package com.github.scala_android.crud

import android.database.sqlite.SQLiteOpenHelper
import android.database.Cursor
import android.content.{ContentValues, Context}
import android.widget.ResourceCursorAdapter
import android.view.View

/**
 * A CrudEntityType for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */

trait SQLiteCrudEntityType extends CrudEntityType[SQLiteCriteria,Cursor,Cursor,ContentValues] {
  def newWritable = new ContentValues

  def openEntityPersistence(crudContext: CrudContext) = new SQLiteEntityPersistence(this, crudContext)

  val cursorVarForListAdapter = new ContextVar[Cursor]

  def createListAdapter(persistence: EntityPersistence[SQLiteCriteria,Cursor,Cursor,ContentValues], crudContext: CrudContext): ResourceCursorAdapter = {
    val criteria = persistence.newCriteria
    copyFields(crudContext.activity.getIntent, criteria)
    val cursor = persistence.findAll(criteria)
    cursorVarForListAdapter.set(crudContext, cursor)
    crudContext.activity.startManagingCursor(cursor)
    new ResourceCursorAdapter(crudContext.context, rowLayout, cursor) {
      def bindView(view: View, context: Context, cursor: Cursor) {
        copyFields(cursor, view)
      }
    }
  }

  def refreshAfterSave(crudContext: CrudContext) {
    cursorVarForListAdapter.get(crudContext).map(_.requery)
  }

  def getDatabaseSetup(context: Context): SQLiteOpenHelper

  override def destroyContextVars(crudContext: CrudContext) {
    cursorVarForListAdapter.clear(crudContext).map(_.close())
    super.destroyContextVars(crudContext)
  }
}
