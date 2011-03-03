package com.github.scala_android.crud

import android.database.sqlite.SQLiteOpenHelper
import android.database.Cursor
import android.content.{ContentValues, Context}

/**
 * A CrudEntityConfig for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */

trait SQLiteCrudEntityConfig extends CrudEntityConfig[SQLiteCriteria,Cursor,Cursor,ContentValues] {
  def openEntityPersistence(context: Context) = new SQLiteEntityPersistence(this, context)

  def getDatabaseSetup(context: Context): SQLiteOpenHelper
}
