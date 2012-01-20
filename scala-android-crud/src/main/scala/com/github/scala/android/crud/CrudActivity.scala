package com.github.scala.android.crud

import action.{OperationResponse, EntityOperation}
import android.os.Bundle
import com.github.triangle.JavaUtil.toRunnable
import com.github.triangle.PortableField
import android.content.Intent
import android.app.Activity
import com.github.scala.android.crud.view.AndroidConversions._
import android.widget.Toast

/** A generic Activity for CRUD operations
  * @author Eric Pabst (epabst@gmail.com)
  */
class CrudActivity(val crudType: CrudType, val application: CrudApplication) extends BaseCrudActivity {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(crudType.entryLayout)
    val currentPath = currentUriPath
    val contextItems = List(currentPath, crudContext, PortableField.UseDefaults)
    if (crudType.maySpecifyEntityInstance(currentPath)) {
      future {
        withPersistence { persistence =>
          val readableOrUnit: AnyRef = persistence.find(currentPath).getOrElse(PortableField.UseDefaults)
          val portableValue = entityType.copyFromItem(readableOrUnit :: contextItems)
          runOnUiThread { portableValue.copyTo(this, contextItems) }
        }
      }
    } else {
      entityType.copyFromItem(PortableField.UseDefaults :: contextItems, this)
    }
  }

  override def onPause() {
    val writable = crudType.newWritable
    withPersistence { persistence =>
      val transformedWritable = entityType.transformWithItem(writable, this :: contextItems)
      saveForOnPause(persistence, transformedWritable)
    }
    super.onPause()
  }

  private[crud] def saveForOnPause(persistence: CrudPersistence, writable: AnyRef) {
    try {
      val id = entityType.IdField.getter(currentUriPath)
      val newId = persistence.save(id, writable)
      Toast.makeText(this, res.R.string.data_saved_notification, Toast.LENGTH_SHORT).show()
      if (id.isEmpty) setIntent(getIntent.setData(uriWithId(newId)))
    } catch { case e => error("onPause: Unable to store " + writable, e) }
  }

  protected def normalActions = crudType.getEntityActions(application).filter {
    case action: EntityOperation => action.entityName != entityType.entityName || action.action != currentAction
    case _ => true
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
      //"this" is included in the list so that existing data isn't cleared.
      entityType.copyFromItem(List(OperationResponse(requestCode, data), crudContext, this), this)
    } else {
      debug("onActivityResult received resultCode of " + resultCode + " and data " + data + " for request " + requestCode)
    }
  }
}
