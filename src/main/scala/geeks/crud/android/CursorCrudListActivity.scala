package geeks.crud.android

import android.os.Bundle
import android.app.{AlertDialog, ListActivity}
import android.widget.{ListAdapter, CursorAdapter, ResourceCursorAdapter}
import android.net.Uri
import android.database.Cursor
import android.view.{View, MenuItem, Menu}
import android.content.{ContentValues, Context, DialogInterface}
import geeks.financial.futurebalance.android.FBDatabaseComponent
import geeks.crud.EntityPersistence

/**
 * A trait for a ListActivity that uses {@link Cursor} and {@link ContentValues}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/12/11
 * Time: 10:25 PM
 */

abstract class CursorCrudListActivity(entityConfig: AndroidEntityCrudConfig)
  extends CrudListActivity[Long,Cursor,Cursor,ContentValues](entityConfig) with SQLiteEntityPersistenceComponent with FBDatabaseComponent {

  def persistence: EntityPersistence[Long,Cursor,Cursor,ContentValues]

  lazy val dataSource: CursorAdapter = {
    val cursor = persistence.findAll
    startManagingCursor(cursor)
    new ResourceCursorAdapter(this, entityConfig.rowLayout, cursor) {
      def bindView(view: View, context: Context, cursor: Cursor) {
        entityConfig.fields.foreach(_.copy(cursor, view))
      }
    }
  }

  def listAdapter: ListAdapter = dataSource

  override def refreshAfterSave() = dataSource.getCursor.requery
}
