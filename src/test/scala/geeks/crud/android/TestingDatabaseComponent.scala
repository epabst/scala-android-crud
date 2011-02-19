package geeks.crud.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * A cake-pattern component for {@link DatabaseSetup}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/9/11
 * Time: 3:40 PM
 */

trait TestingDatabaseComponent {
  def context: Context

  lazy val databaseSetup = new DatabaseSetup

  /**
   * Sets up the database.
   * @author Eric Pabst (epabst@gmail.com)
   * Date: 2/2/11
   * Time: 11:18 PM
   */
  class DatabaseSetup extends SQLiteOpenHelper(context, "TestDatabase", null, 1) {

    def onCreate(db: SQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS Person ("
          + BaseColumns._ID
          + " INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, age INTEGER)")
    }

    def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      // Steps to upgrade the database for the new version ...
    }
  }
}