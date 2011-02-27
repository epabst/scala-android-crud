package geeks.crud.android

import _root_.android.app.{Activity, AlertDialog}
import android.content.Intent
import android.os.Bundle
import android.view.{View, MenuItem, Menu}
import android.content.{Context, DialogInterface}
import geeks.crud._

/**
 * A generic ListActivity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 * @param Q the query criteria type
 * @param L the type of findAll (e.g. Cursor)
 * @param R the type to read from (e.g. Cursor)
 * @param W the type to write to (e.g. ContentValues)
 */
class CrudActivity[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef](val entityConfig: CrudEntityConfig[Q,L,R,W])
  extends Activity with CrudContext[Q,L,R,W] {

  def id: Long = getIntent.getData.getLastPathSegment.toLong

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(entityConfig.entryLayout)

    val readable = persistence.find(id)
    entityConfig.copyFields(readable, this)
  }

  override def onStop() {
    val writable = persistence.newWritable
    entityConfig.copyFields(this, writable)
    persistence.save(Some(id), writable)
    persistence.close()
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    //menu.add(0, ADD_DIALOG_ID, 1, entityConfig.addItemString)
    true
  }

  override def onMenuItemSelected(featureId: Int, item: MenuItem): Boolean = {
//    if (item.getItemId == ADD_DIALOG_ID) {
//      showDialog(ADD_DIALOG_ID)
//    }
    true
  }
}
