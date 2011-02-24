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
 * @param Q the query criteria type
 */
abstract class CursorCrudListActivity[Q <: AnyRef](entityConfig: AndroidEntityCrudConfig)
  extends CrudListActivity[Long,Q,Cursor,Cursor,ContentValues](entityConfig) with SQLiteEntityPersistenceComponent with FBDatabaseComponent {

  def persistence: EntityPersistence[Long,Q,Cursor,Cursor,ContentValues]

  lazy val dataSource: CursorAdapter = {
    val criteria = persistence.newCriteria
    entityConfig.copyFields(getIntent, criteria)
    val cursor = persistence.findAll(criteria)
    startManagingCursor(cursor)
    new ResourceCursorAdapter(this, entityConfig.rowLayout, cursor) {
      def bindView(view: View, context: Context, cursor: Cursor) {
        entityConfig.copyFields(cursor, view)
      }
    }
  }

  def listAdapter: ListAdapter = dataSource

  override def refreshAfterSave() = dataSource.getCursor.requery
}
