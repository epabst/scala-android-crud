package com.github.scala.android.crud.generate

import com.github.scala.android.crud.view.AndroidResourceAnalyzer._
import com.github.triangle.{PortableField, FieldList, BaseField}
import com.github.scala.android.crud.persistence.{EntityType, CursorField}
import com.github.scala.android.crud.view._
import xml.NodeSeq
import com.github.scala.android.crud.NamingConventions

case class EntityFieldInfo(field: BaseField, rIdClasses: Seq[Class[_]]) {
  private lazy val updateablePersistedFields = CursorField.updateablePersistedFields(field, rIdClasses)

  private def viewFields(field: BaseField): List[ViewField[_]] = field.deepCollect {
    case matchingField: ViewField[_] => matchingField
  }

  lazy val viewIdFieldInfos: List[ViewIdFieldInfo] = field.deepCollect {
    case viewIdField: ViewIdField[_] => ViewIdFieldInfo(viewIdField.viewRef.fieldName(rIdClasses), viewIdField)
  }

  lazy val isDisplayable: Boolean = !displayableViewIdFieldInfos.isEmpty
  def isPersisted: Boolean = !updateablePersistedFields.isEmpty
  def isUpdateable: Boolean = isDisplayable && isPersisted

  lazy val nestedEntityTypeViewInfos: List[EntityTypeViewInfo] = field.deepCollect {
    case entityView: EntityView => EntityTypeViewInfo(entityView.entityType)
  }

  lazy val displayableViewIdFieldInfos: List[ViewIdFieldInfo] =
    viewIdFieldInfos.filter(_.layout.displayXml != NodeSeq.Empty) ++ nestedEntityTypeViewInfos.flatMap(_.displayableViewIdFieldInfos)

  lazy val updateableViewIdFieldInfos: List[ViewIdFieldInfo] =
    if (isPersisted) viewIdFieldInfos.filter(_.layout.editXml != NodeSeq.Empty) else Nil

  lazy val otherViewFields = {
    val viewFieldsWithinViewIdFields = viewFields(FieldList(viewIdFieldInfos.map(_.field):_*))
    viewFields(field).filterNot(viewFieldsWithinViewIdFields.contains)
  }
}

case class ViewIdFieldInfo(id: String, displayName: String, field: PortableField[_]) {
  lazy val viewFields: List[ViewField[_]] = field.deepCollect {
    case matchingField: ViewField[_] => matchingField
  }

  def layout: FieldLayout = viewFields.headOption.map(_.defaultLayout).getOrElse(FieldLayout.noLayout)
}

object ViewIdFieldInfo {
  def apply(id: String, viewField: PortableField[_]): ViewIdFieldInfo =
    ViewIdFieldInfo(id, FieldLayout.toDisplayName(id), viewField)
}

case class EntityTypeViewInfo(entityType: EntityType) {
  def entityName = entityType.entityName
  lazy val layoutPrefix = NamingConventions.toLayoutPrefix(entityType.entityName)
  lazy val rIdClasses: Seq[Class[_]] = detectRIdClasses(entityType.getClass)
  lazy val entityFieldInfos: List[EntityFieldInfo] = entityType.fields.map(EntityFieldInfo(_, rIdClasses))
  lazy val displayableViewIdFieldInfos: List[ViewIdFieldInfo] = entityFieldInfos.flatMap(_.displayableViewIdFieldInfos)
  lazy val updateableViewIdFieldInfos: List[ViewIdFieldInfo] = entityFieldInfos.flatMap(_.updateableViewIdFieldInfos)
  def isUpdateable: Boolean = !updateableViewIdFieldInfos.isEmpty
}
