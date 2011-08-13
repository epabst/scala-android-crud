package com.github.scala_android.crud

import _root_.android.app.Activity
import _root_.android.view.{Menu, MenuItem}
import android.os.Bundle
import com.github.triangle.ValueFormat.basicFormat
import com.github.triangle.JavaUtil.toRunnable
import persistence.CrudPersistence

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
class CrudActivity(val entityType: CrudType, val application: CrudApplication)
  extends Activity with BaseCrudActivity {

  private val idFormat = basicFormat[ID]
  def id: Option[ID] = Option(getIntent).flatMap(intent => idFormat.toValue(intent.getData.getLastPathSegment))

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(entityType.entryLayout)
    val contextItems = List(getIntent, crudContext, Unit)
    future {
      withPersistence { persistence =>
        val readableOrUnit: AnyRef = id.map(i => persistence.find(i).get).getOrElse(Unit)
        val portableValue = entityType.copyFromItem(readableOrUnit :: contextItems)
        runOnUiThread { portableValue.copyTo(this) }
      }
    }
  }

  override def onPause() {
    //intentionally don't include CrudContext presumably those are only used for calculated fields, which shouldn't be persisted.
    val contextItems = List(getIntent, Unit)
    val writable = entityType.newWritable
    withPersistence { persistence =>
      val transformedWritable = entityType.transformWithItem(writable, this :: contextItems)
      saveForOnPause(persistence, transformedWritable)
    }
    super.onPause()
  }

  private[crud] def saveForOnPause(persistence: CrudPersistence, writable: AnyRef) {
    try { persistence.save(id, writable) }
    catch { case e => error("onPause: Unable to store " + writable, e) }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    //todo add revert support
    //todo add support for crud actions
    //menu.add(0, ADD_DIALOG_ID, 1, entityType.addItemString)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
//    if (item.getItemId == ADD_DIALOG_ID) {
//      showDialog(ADD_DIALOG_ID)
//    }
    true
  }
}
