package com.github.scala.android.crud

import action.{Action, EntityAction}
import android.os.Bundle
import com.github.triangle.ValueFormat.basicFormat
import com.github.triangle.JavaUtil.toRunnable

/**
 * A generic Activity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 */
class CrudActivity(val entityType: CrudType, val application: CrudApplication) extends BaseCrudActivity {

  private val idFormat = basicFormat[ID]
  def id: Option[ID] = currentUriPath.segments.lastOption.flatMap(idFormat.toValue(_))

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(entityType.entryLayout)
    val contextItems = List(currentUriPath, crudContext, Unit)
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
    val contextItems = List(currentUriPath, Unit)
    val writable = entityType.newWritable
    withPersistence { persistence =>
      val transformedWritable = entityType.transformWithItem(writable, this :: contextItems)
      saveForOnPause(persistence, transformedWritable)
    }
    super.onPause()
  }

  private[crud] def saveForOnPause(persistence: CrudPersistence, writable: AnyRef) {
    try {
      val newId = persistence.save(id, writable)
      if (id.isEmpty) setIntent(getIntent.setData(Action.toUri(currentUriPath.specify(entityType.toUri(newId).segments:_*))))
    } catch { case e => error("onPause: Unable to store " + writable, e) }
  }

  protected def applicableActions = entityType.getEntityActions(application).filter {
    case action: EntityAction => action.entityName != entityType.entityName || action.action != currentAction
    case _ => true
  }
}
