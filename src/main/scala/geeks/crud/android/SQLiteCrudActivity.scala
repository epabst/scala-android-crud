package geeks.crud.android

import android.database.Cursor
import android.content.ContentValues

/**
 * A trait for a ListActivity that uses {@link Cursor} and {@link ContentValues}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/12/11
 * Time: 10:25 PM
 * @param Q the query criteria type
 */
class SQLiteCrudActivity(entityConfig: SQLiteCrudEntityConfig)
  extends CrudActivity[SQLiteCriteria,Cursor,Cursor,ContentValues](entityConfig)
