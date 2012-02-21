package com.github.scala.android.crud

import common.UriPath
import common.Common
import persistence._
import common.PlatformTypes._
import com.github.triangle.Logging

/** An EntityPersistence for a CrudType.
  * @author Eric Pabst (epabst@gmail.com)
  */
trait CrudPersistence extends EntityPersistence with Logging {
  override protected def logTag: String = Common.tryToEvaluate(entityType.logTag).getOrElse(Common.logTag)

  def entityType: EntityType

  def crudContext: CrudContext

  def toUri(id: ID) = entityType.toUri(id)

  def find[T <: AnyRef](uri: UriPath, instantiateItem: => T): Option[T] =
    find(uri).map(entityType.fieldsIncludingIdPk.copyAndTransform(_, instantiateItem))

  /** Find an entity with a given ID using a baseUri. */
  def find(id: ID, baseUri: UriPath): Option[AnyRef] = find(baseUri.specify(entityType.entityName, id.toString))

  override def find(uri: UriPath): Option[AnyRef] = {
    val result = super.find(uri)
    info("find(" + uri + ") for " + entityType.entityName + " returned " + result)
    result
  }

  def findAll[T <: AnyRef](uri: UriPath, instantiateItem: => T): Seq[T] =
    findAll(uri).map(entityType.fieldsIncludingIdPk.copyAndTransform(_, instantiateItem))

  /** Saves the entity.  This assumes that the entityType's fields support copying from the given modelEntity. */
  def save(modelEntity: IdPk): ID = {
    val writable = newWritable
    save(modelEntity.id, entityType.copyAndTransform(modelEntity, writable))
  }
}

trait SeqCrudPersistence[T <: AnyRef] extends SeqEntityPersistence[T] with CrudPersistence

class ListBufferCrudPersistence[T <: AnyRef](newWritableFunction: => T, val entityType: EntityType, val crudContext: CrudContext)
        extends ListBufferEntityPersistence[T](newWritableFunction) with SeqCrudPersistence[T]
