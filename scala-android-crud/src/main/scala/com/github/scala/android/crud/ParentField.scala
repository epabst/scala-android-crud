package com.github.scala.android.crud

import common.PlatformTypes._
import persistence.CursorField._
import android.provider.BaseColumns
import com.github.triangle.{BaseField, DelegatingPortableField}
import persistence.EntityType

/** A ParentField to a CrudType.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class ParentField(entityType: EntityType) extends DelegatingPortableField[ID] {
  val fieldName = entityType.entityName.toLowerCase + BaseColumns._ID

  protected val delegate = entityType.UriPathId

  override def toString = "ParentField(" + entityType.entityName + ")"
}

object ParentField {
  def parentFields(field: BaseField): List[ParentField] = field.deepCollect {
    case parentField: ParentField => parentField
  }

  def foreignKey(entityType: EntityType) = {
    val parentField = ParentField(entityType)
    parentField + persisted[ID](parentField.fieldName) + sqliteCriteria[ID](parentField.fieldName)
  }
}