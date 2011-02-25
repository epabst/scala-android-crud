package geeks.crud.android

import android.database.sqlite.SQLiteOpenHelper
import android.content.Context

/**
 * A CrudEntityConfig for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */

trait SQLiteCrudEntityConfig extends AndroidCrudEntityConfig[Long] {
  def getDatabaseSetup(context: Context): SQLiteOpenHelper
}

trait SQLiteUIActionFactory extends AndroidUIActionFactory[Long]
