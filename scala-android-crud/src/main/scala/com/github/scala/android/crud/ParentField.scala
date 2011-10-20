package com.github.scala.android.crud

import common.PlatformTypes
import persistence.CursorField._
import android.provider.BaseColumns
import com.github.triangle.{BaseField, DelegatingPortableField}

/**
 * A ParentField to a CrudType.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/11/11
 * Time: 11:35 PM
 */
case class ParentField(entityType: CrudType) extends PlatformTypes with DelegatingPortableField[ID] {
  val fieldName = entityType.entityName.toLowerCase + BaseColumns._ID

  protected val delegate = entityType.UriPathId

  override def toString = "ParentField(" + entityType.entityName + ")"
}

object ParentField {
  def parentFields(field: BaseField): List[ParentField] = field.deepCollect {
    case parentField: ParentField => parentField
  }

  def foreignKey(entityType: CrudType) = {
    val parentField = ParentField(entityType)
    parentField + persisted[ID](parentField.fieldName) + sqliteCriteria[ID](parentField.fieldName)
  }
}