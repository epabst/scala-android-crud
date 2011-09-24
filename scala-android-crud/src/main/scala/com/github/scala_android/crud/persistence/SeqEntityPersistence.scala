package com.github.scala_android.crud.persistence

import scala.collection.mutable
import IdPk._
import com.github.scala_android.crud.action.UriPath

/**
 * EntityPersistence for a simple generated Seq.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

trait SeqEntityPersistence[T <: AnyRef] extends EntityPersistence {
  override def find(id: ID): Option[T] = {
    findAll(toUri(id)).find(entity => id == idField(entity)).asInstanceOf[Option[T]]
  }

  protected def doSave(id: Option[ID], data: AnyRef): ID = throw new UnsupportedOperationException("write not supported")

  protected def doDelete(ids: Seq[ID]) { throw new UnsupportedOperationException("delete not supported") }

  def close() {}
}

trait ListBufferEntityPersistence[T <: AnyRef] extends SeqEntityPersistence[T] {
  val buffer = mutable.ListBuffer[T]()

  var nextId = 10000L

  def findAll(uri: UriPath) = buffer.toList

  override protected def doSave(id: Option[ID], item: AnyRef) = {
    val newId = id.getOrElse {
      nextId += 1
      nextId
    }
    buffer += idField.transformer(item.asInstanceOf[T])(Some(newId));
    newId
  }

  override protected def doDelete(ids: Seq[ID]) {
    ids.foreach(id => find(id).map(entity => buffer -= entity))
  }
}