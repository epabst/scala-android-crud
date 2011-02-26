package geeks.crud.android

import android.os.Bundle
import android.app.{AlertDialog, ListActivity}
import android.widget.{ListAdapter, CursorAdapter, ResourceCursorAdapter}
import android.database.Cursor
import android.view.{View, MenuItem, Menu}
import android.content.{ContentValues, Context, DialogInterface}

/**
 * A trait for a ListActivity that uses {@link Cursor} and {@link ContentValues}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/12/11
 * Time: 10:25 PM
 * @param Q the query criteria type
 */
class SQLiteCrudActivity(entityConfig: SQLiteCrudEntityConfig)
  extends CrudActivity[SQLiteCriteria,Cursor,Cursor,ContentValues](entityConfig)
