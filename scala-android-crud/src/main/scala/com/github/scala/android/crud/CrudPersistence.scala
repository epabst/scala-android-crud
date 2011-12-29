package com.github.scala.android.crud

import common.UriPath
import common.Common
import persistence._
import com.github.triangle.{Setter, Getter, FieldList}
import common.PlatformTypes._

/** An EntityPersistence for a CrudType.
  * @author Eric Pabst (epabst@gmail.com)
  */
trait CrudPersistence extends EntityPersistence {
  protected def logTag: String = Common.tryToEvaluate(entityType.logTag).getOrElse(Common.logTag)

  def entityType: EntityType

  def crudContext: CrudContext

  def toUri(id: ID) = entityType.toUri(id)

  private lazy val idPkField = entityType.IdField + Getter[IdPk,ID](_.id).withTransformer(e => e.id(_)) +
    Setter((e: MutableIdPk) => e.id = _)
  private lazy val fieldsIncludingIdPk = FieldList((idPkField +: entityType.fields): _*)

  def find[T <: AnyRef](uri: UriPath, instantiateItem: => T): Option[T] =
    find(uri).map(fieldsIncludingIdPk.transform(instantiateItem, _))

  def findAll[T <: AnyRef](uri: UriPath, instantiateItem: => T): Seq[T] =
    findAll(uri).map(fieldsIncludingIdPk.transform(instantiateItem, _))

  /** Saves the entity.  This assumes that the entityType's fields support copying from the given modelEntity. */
  def save(modelEntity: IdPk): ID = {
    val writable = newWritable
    save(modelEntity.id, entityType.transform(writable, modelEntity))
  }
}

trait SeqCrudPersistence[T <: AnyRef] extends SeqEntityPersistence[T] with CrudPersistence

class ListBufferCrudPersistence[T <: AnyRef](newWritableFunction: => T, val entityType: EntityType, val crudContext: CrudContext)
        extends ListBufferEntityPersistence[T](newWritableFunction) with SeqCrudPersistence[T]
