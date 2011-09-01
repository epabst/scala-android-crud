package com.github.scala_android.crud

import common.PlatformTypes
import persistence.CursorField._
import android.provider.BaseColumns
import com.github.triangle.{BaseField, DelegatingPortableField}

/**
 * A ForeignKey to a CrudType.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/11/11
 * Time: 11:35 PM
 */
case class ForeignKey(entityType: CrudType) extends PlatformTypes with DelegatingPortableField[ID] {
  val fieldName = entityType.entityName.toLowerCase + BaseColumns._ID

  protected val delegate = persisted[ID](fieldName) + entityType.intentIdField + sqliteCriteria[ID](fieldName)

  override def toString = "ForeignKey(" + entityType.entityName + ")"
}

object ForeignKey {

  def foreignKeys(field: BaseField): List[ForeignKey] = field.deepCollect {
    case foreignKey: ForeignKey => foreignKey
  }
}