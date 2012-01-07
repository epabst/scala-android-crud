package com.github.scala.android.crud.generate

import android.view.View
import com.github.scala.android.crud.view.AndroidResourceAnalyzer._
import com.github.triangle.{PortableField, FieldList, SubjectField, BaseField}
import com.github.scala.android.crud.view.{FieldLayout, ViewField, ViewIdNameField, ViewIdField}
import com.github.scala.android.crud.persistence.{EntityType, CursorField}

case class EntityFieldInfo(field: BaseField, rIdClasses: Seq[Class[_]]) {
  lazy val updateablePersistedFields = CursorField.updateablePersistedFields(field, rIdClasses)
  lazy val persistedFieldOption = updateablePersistedFields.headOption

  lazy val viewIdFields: List[ViewIdField[_]] = field.deepCollect[ViewIdField[_]] {
    case matchingField: ViewIdField[_] => matchingField
  }
  lazy val viewIdNameFields: List[ViewIdNameField[_]] = field.deepCollect[ViewIdNameField[_]] {
    case matchingField: ViewIdNameField[_] => matchingField
  }

  private[generate] def fieldsWithViewSubject(field: BaseField): List[SubjectField] = field.deepCollect[SubjectField] {
    case matchingField: SubjectField if classOf[View].isAssignableFrom(matchingField.subjectManifest.erasure) => {
      matchingField
    }
  }

  lazy val viewFieldsWithId = this.fieldsWithViewSubject(FieldList.toFieldList(viewIdFields))
  lazy val otherViewFields = this.fieldsWithViewSubject(field).filterNot(viewFieldsWithId.contains)
  lazy val viewFieldInfos: List[ViewFieldInfo] = viewIdNameFields.map(f => ViewFieldInfo(f.viewResourceIdName, f)) ++
    viewIdFields.map { viewIdField =>
      ViewFieldInfo(resourceFieldWithIntValue(rIdClasses, viewIdField.viewResourceId).getName, viewIdField)
    }

  lazy val displayable = !viewFieldInfos.isEmpty
  lazy val updateable = displayable && persistedFieldOption.isDefined
}

case class ViewFieldInfo(id: String, displayName: String, field: PortableField[_]) {
  lazy val viewFields: List[ViewField[_]] = field.deepCollect {
    case matchingField: ViewField[_] => matchingField
  }

  def firstViewField = viewFields.headOption.getOrElse(Predef.error("No ViewField in " + this))
  def layout = firstViewField.defaultLayout
}

object ViewFieldInfo {
  def apply(id: String, viewField: PortableField[_]): ViewFieldInfo = ViewFieldInfo(id, FieldLayout.toDisplayName(id), viewField)
}

case class EntityTypeViewInfo(entityType: EntityType) {
  lazy val rIdClasses: Seq[Class[_]] = detectRIdClasses(entityType.getClass)
  lazy val entityFieldInfos = entityType.fields.map(EntityFieldInfo(_, rIdClasses))
  lazy val displayableViewFieldInfos = entityFieldInfos.filter(_.displayable).flatMap(_.viewFieldInfos)
  lazy val updateableEntityFieldInfos = entityFieldInfos.filter(_.updateable)
  def isUpdateable: Boolean = !updateableEntityFieldInfos.isEmpty
  lazy val updateableViewFieldInfos = updateableEntityFieldInfos.flatMap(_.viewFieldInfos)
}
