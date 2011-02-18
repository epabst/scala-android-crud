package geeks.crud.android

import android.os.Bundle
import android.app.{AlertDialog, ListActivity}
import android.widget.{ListAdapter, CursorAdapter, ResourceCursorAdapter}
import geeks.crud.EntityPersistenceComponent
import android.net.Uri
import android.database.Cursor
import android.view.{View, MenuItem, Menu}
import android.content.{ContentValues, Context, DialogInterface}

/**
 * A trait for a ListActivity that uses {@link Cursor} and {@link ContentValues}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/12/11
 * Time: 10:25 PM
 */

trait CursorCrudListActivity extends CrudListActivity[Cursor,Cursor,ContentValues] {
  lazy val dataSource: CursorAdapter = {
    val cursor = persistence.findAll
    new ResourceCursorAdapter(this, rowLayout, cursor) {
      def bindView(view: View, context: Context, cursor: Cursor) {
        fields.foreach(_.copy(cursor, view))
      }
    }
  }

  def listAdapter: ListAdapter = dataSource

  override def refreshAfterSave() = dataSource.getCursor.requery
}
