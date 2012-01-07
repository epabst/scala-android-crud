package com.github.scala.android.crud.generate

import com.github.scala.android.crud.persistence.CursorField
import android.view.View
import com.github.scala.android.crud.view.AndroidResourceAnalyzer._
import com.github.triangle.{PortableField, FieldList, SubjectField, BaseField}
import com.github.scala.android.crud.view.{FieldLayout, ViewField, ViewIdNameField, ViewIdField}
import com.github.scala.android.crud.CrudType

case class NamedField(name: String, field: PortableField[_], displayName: String)

object NamedField {
  def apply(name: String, viewField: PortableField[_]): NamedField = NamedField(name, viewField, FieldLayout.toDisplayName(name))
}

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
  lazy val namedViewFields = viewIdNameFields.map(f => NamedField(f.viewResourceIdName, f)) ++ viewIdFields.map { f =>
    NamedField(findResourceFieldWithIntValue(rIdClasses, f.viewResourceId).map(_.getName).getOrElse {
      throw new IllegalStateException("Unable to find R.id with value " + f.viewResourceId + " in " + rIdClasses.mkString(", "))
    }, f)
  }
  lazy val persistedFieldsWithTypes =
    updateablePersistedFields.map(p => p.toString + ":" + p.persistedType.valueManifest.erasure.getSimpleName)

  lazy val namedFields: List[NamedField] = if (namedViewFields.isEmpty) updateablePersistedFields.map(f => NamedField(f.name, f)) else namedViewFields

  private[generate] def viewFields(field: BaseField): List[ViewField[_]] = field.deepCollect {
    case matchingField: ViewField[_] => matchingField
  }

  lazy val viewFieldInfos: List[ViewFieldInfo] = namedFields.map { namedField =>
    val fieldLayout = viewFields(namedField.field).headOption.map(_.defaultLayout).getOrElse(namedField.field.deepCollect {
      case _: PortableField[Double] => FieldLayout.doubleLayout
      case _: PortableField[String] => FieldLayout.nameLayout
      case _: PortableField[Int] => FieldLayout.intLayout
    }.head)
    val displayable = !namedViewFields.isEmpty
    ViewFieldInfo(namedField.displayName, fieldLayout, namedField.name, displayable,
      updateable = displayable && persistedFieldOption.isDefined)
  }
}

case class ViewFieldInfo(displayName: String, layout: FieldLayout, id: String, displayable: Boolean, updateable: Boolean)

case class CrudTypeInfo(crudType: CrudType) {
  lazy val fieldInfos: List[ViewFieldInfo] = crudType.entityType.fields.flatMap(EntityFieldInfo(_, crudType.rIdClasses).viewFieldInfos)
  lazy val displayFields = fieldInfos.filter(_.displayable)
  lazy val updateableFields = fieldInfos.filter(_.updateable)
}
