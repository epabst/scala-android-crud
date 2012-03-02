package com.github.scala.android.crud

import action.{OperationResponse, EntityOperation}
import android.os.Bundle
import com.github.triangle.PortableField
import android.content.Intent
import android.app.Activity
import com.github.scala.android.crud.view.AndroidConversions._
import android.widget.Toast
import common.UriPath
import validate.ValidationResult

/** A generic Activity for CRUD operations
  * @author Eric Pabst (epabst@gmail.com)
  */
class CrudActivity(val crudType: CrudType, val application: CrudApplication) extends BaseCrudActivity { self =>

  private def populateFromUri(uri: UriPath) {
    future {
      withPersistence { persistence =>
        val readableOrUnit: AnyRef = persistence.find(uri).getOrElse(PortableField.UseDefaults)
        val portableValue = entityType.copyFromItem(readableOrUnit :: contextItems)
        runOnUiThread(this) { portableValue.copyTo(this, contextItems) }
      }
    }
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState == null) {
      setContentView(crudType.entryLayout)
      val currentPath = currentUriPath
      if (crudType.maySpecifyEntityInstance(currentPath)) {
        populateFromUri(currentPath)
      } else {
        entityType.copyFromItem(PortableField.UseDefaults :: contextItems, this)
      }
    }
    if (crudType.maySpecifyEntityInstance(currentUriPath)) {
      crudContext.addCachedStateListener(new CachedStateListener {
        def onClearState(stayActive: Boolean) {
          if (stayActive) {
            populateFromUri(currentUriPath)
          }
        }

        def onSaveState(outState: Bundle) {
          entityType.copy(this, outState)
        }

        def onRestoreState(savedInstanceState: Bundle) {
          val portableValue = entityType.copyFrom(savedInstanceState)
          runOnUiThread(self) { portableValue.copyTo(this, contextItems) }
        }
      })
    }
  }

  override def onBackPressed() {
    // Save before going back so that the Activity being activated will read the correct data from persistence.
    val writable = crudType.newWritable
    withPersistence { persistence =>
      val copyableFields = entityType.copyableTo(writable, contextItemsWithoutUseDefaults)
      val portableValue = copyableFields.copyFromItem(this :: contextItemsWithoutUseDefaults)
      if (portableValue.transform(ValidationResult.Valid).isValid) {
        val transformedWritable = portableValue.transform(writable)
        saveBasedOnUserAction(persistence, transformedWritable)
      } else {
        Toast.makeText(this, res.R.string.data_not_saved_since_invalid_notification, Toast.LENGTH_SHORT).show()
      }
    }
    super.onBackPressed()
  }

  private[crud] def saveBasedOnUserAction(persistence: CrudPersistence, writable: AnyRef) {
    try {
      val id = entityType.IdField.getter(currentUriPath)
      val newId = persistence.save(id, writable)
      Toast.makeText(this, res.R.string.data_saved_notification, Toast.LENGTH_SHORT).show()
      if (id.isEmpty) setIntent(getIntent.setData(uriWithId(newId)))
    } catch { case e => logError("onPause: Unable to store " + writable, e) }
  }

  protected def normalActions = application.actionsForEntity(entityType).filter {
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
