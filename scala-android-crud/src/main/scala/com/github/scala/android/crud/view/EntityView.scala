package com.github.scala.android.crud.view

import com.github.scala.android.crud.persistence.EntityType
import com.github.scala.android.crud.common.PlatformTypes.ID
import com.github.triangle.PortableField._
import com.github.triangle.{SetterUsingItems, Getter}
import com.github.scala.android.crud.GeneratedCrudType.{UriField, CrudContextField}
import com.github.triangle.&&
import scala.collection.JavaConversions._
import android.widget._
import android.view.View
import android.app.Activity
import xml.NodeSeq

/** A ViewField that allows choosing a specific entity of a given EntityType or displaying its fields' values.
  * The layout for the EntityType that contains this EntityView may refer to fields of this view's EntityType
  * in the same way as referring to its own fields.  If both have a field of the same name, the behavior is undefined.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class EntityView(entityType: EntityType)
  extends ViewField[ID](FieldLayout(displayXml = NodeSeq.Empty, editXml = <Spinner android:drawSelectorOnTop = "true"/>)) {

  protected val itemViewResourceId = _root_.android.R.layout.simple_spinner_dropdown_item

  private object AndroidUIElement {
    def unapply(target: AnyRef): Option[AnyRef] = target match {
      case view: View => Some(view)
      case activity: Activity => Some(activity)
      case _ => None
    }
  }

  protected val delegate = Getter[AdapterView[BaseAdapter], ID](v => Option(v.getSelectedItemId)) + SetterUsingItems[ID] {
    case (adapterView: AdapterView[BaseAdapter], UriField(Some(uri)) && CrudContextField(Some(crudContext))) => idOpt: Option[ID] =>
      if (idOpt.isDefined || adapterView.getAdapter == null) {
        crudContext.withEntityPersistence(entityType) { persistence =>
          object FindAllResult {
            // Only call findAll if actually needed, and only once if needed for multiple purposes
            lazy val seq = persistence.findAll(uri)
          }
          //don't do it again if already done from a previous time
          if (adapterView.getAdapter == null) {
            val adapter = new ArrayAdapter[AnyRef](adapterView.getContext, itemViewResourceId, FindAllResult.seq)
            adapterView.setAdapter(adapter)
          }
          if (idOpt.isDefined) {
            val position = FindAllResult.seq.view.map(item => persistence.idPkField(item)).indexOf(idOpt.get)
            adapterView.setSelection(position)
          }
        }
      }
    case (AndroidUIElement(uiElement), items @ UriField(Some(baseUri)) && CrudContextField(Some(crudContext))) => idOpt: Option[ID] =>
      val entityOpt = idOpt.flatMap(id => crudContext.withEntityPersistence(entityType)(_.find(id, baseUri)))
      entityType.copyFromItem(entityOpt.getOrElse(UseDefaults) +: items, uiElement)
  }

  override def toString = "EntityView(" + entityType.entityName + ")"
}
