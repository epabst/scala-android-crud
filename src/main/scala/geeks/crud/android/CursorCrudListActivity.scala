package geeks.crud.android

import android.os.Bundle
import android.app.{AlertDialog, ListActivity}
import android.widget.{SimpleCursorAdapter, ListAdapter, CursorAdapter}
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
    val adapter = new SimpleCursorAdapter(this, rowLayout, cursor,
      //provide the field names but making sure that they have the same length as the viewResourceIds.
      //These aren't actually used by the ViewBinder below.
      (cursor.getColumnNames.toList.tail ::: cursor.getColumnNames.toList.tail).slice(0, viewResourceIds.size).toArray,
      viewResourceIds.toArray)
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      def setViewValue(view: View, cursor: Cursor, columnIndex: Int) = {
        //todo fix this to really correspond!!!
        fields.foreach(field => if (field.queryFieldNames.contains(cursor.getColumnName(columnIndex))) {
          field match {
            case viewField: ViewField[Cursor,ContentValues] => viewField.readIntoFieldView(cursor, view)
            case _ =>
          }
        })
        true
      }
    })
    adapter
  }

  def listAdapter: ListAdapter = dataSource

  override def refreshAfterSave() = dataSource.getCursor.requery
}
