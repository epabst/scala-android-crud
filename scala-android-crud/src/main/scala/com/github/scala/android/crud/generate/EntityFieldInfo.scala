package com.github.scala.android.crud.generate

import android.view.View
import com.github.scala.android.crud.view.AndroidResourceAnalyzer._
import com.github.triangle.{PortableField, FieldList, SubjectField, BaseField}
import com.github.scala.android.crud.view.{FieldLayout, ViewField, ViewIdNameField, ViewIdField}
import com.github.scala.android.crud.persistence.{EntityType, CursorField}

case class EntityFieldInfo(field: BaseField, rIdClasses: Seq[Class[_]]) {
  private lazy val updateablePersistedFields = CursorField.updateablePersistedFields(field, rIdClasses)

  private lazy val viewIdFields: List[ViewIdField[_]] = field.deepCollect[ViewIdField[_]] {
    case matchingField: ViewIdField[_] => matchingField
  }
  private lazy val viewIdNameFields: List[ViewIdNameField[_]] = field.deepCollect[ViewIdNameField[_]] {
    case matchingField: ViewIdNameField[_] => matchingField
  }

  private[generate] def fieldsWithViewSubject(field: BaseField): List[SubjectField] = field.deepCollect[SubjectField] {
    case matchingField: SubjectField if classOf[View].isAssignableFrom(matchingField.subjectManifest.erasure) => {
      matchingField
    }
  }

  private lazy val viewFieldsWithId = this.fieldsWithViewSubject(FieldList.toFieldList(viewIdFields))
  lazy val otherViewFields = this.fieldsWithViewSubject(field).filterNot(viewFieldsWithId.contains)
  lazy val viewIdFieldInfos: List[ViewIdFieldInfo] = viewIdNameFields.map(f => ViewIdFieldInfo(f.viewResourceIdName, f)) ++
    viewIdFields.map { viewIdField =>
      ViewIdFieldInfo(resourceFieldWithIntValue(rIdClasses, viewIdField.viewResourceId).getName, viewIdField)
    }

  lazy val isDisplayable: Boolean = !viewIdFieldInfos.isEmpty
  def isPersisted: Boolean = !updateablePersistedFields.isEmpty
  def isUpdateable: Boolean = isDisplayable && isPersisted

  lazy val displayableViewIdFieldInfos: List[ViewIdFieldInfo] = if (isDisplayable) viewIdFieldInfos else Nil
  lazy val updateableViewIdFieldInfos: List[ViewIdFieldInfo] = if (isUpdateable) viewIdFieldInfos else Nil
}

case class ViewIdFieldInfo(id: String, displayName: String, field: PortableField[_]) {
  lazy val viewFields: List[ViewField[_]] = field.deepCollect {
    case matchingField: ViewField[_] => matchingField
  }

  def firstViewField = viewFields.headOption.getOrElse(Predef.error("No ViewField in " + this))
  def layout = firstViewField.defaultLayout
}

object ViewIdFieldInfo {
  def apply(id: String, viewField: PortableField[_]): ViewIdFieldInfo =
    ViewIdFieldInfo(id, FieldLayout.toDisplayName(id), viewField)
}

case class EntityTypeViewInfo(entityType: EntityType) {
  lazy val rIdClasses: Seq[Class[_]] = detectRIdClasses(entityType.getClass)
  lazy val entityFieldInfos: List[EntityFieldInfo] = entityType.fields.map(EntityFieldInfo(_, rIdClasses))
  lazy val displayableViewIdFieldInfos: List[ViewIdFieldInfo] = entityFieldInfos.flatMap(_.displayableViewIdFieldInfos)
  lazy val updateableViewIdFieldInfos: List[ViewIdFieldInfo] = entityFieldInfos.flatMap(_.updateableViewIdFieldInfos)
  def isUpdateable: Boolean = !updateableViewIdFieldInfos.isEmpty
}
