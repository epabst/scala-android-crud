package com.github.scala.android.crud.persistence

import scala.collection.mutable
import com.github.scala.android.crud.action.UriPath
import com.github.triangle.{Setter, Getter, Field}

/**
 * EntityPersistence for a simple generated Seq.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

trait SeqEntityPersistence[T <: AnyRef] extends EntityPersistence {
  protected def doSave(id: Option[ID], data: AnyRef): ID = throw new UnsupportedOperationException("write not supported")

  protected def doDelete(uri: UriPath) { throw new UnsupportedOperationException("delete not supported") }

  def close() {}
}

trait ListBufferEntityPersistence[T <: AnyRef] extends SeqEntityPersistence[T] {
  private object IdField extends Field[ID](Getter[IdPk,ID](_.id).withTransformer(e => e.id(_)) +
      Setter((e: MutableIdPk) => e.id = _) + CursorField.PersistedId)
  val buffer = mutable.ListBuffer[T]()

  var nextId = 10000L

  //todo only return the one that matches the ID in the uri, if present
  //def findAll(uri: UriPath) = buffer.toList.filter(item => uri.segments.containsSlice(toUri(IdField(item)).segments))
  def findAll(uri: UriPath) = buffer.toList

  override protected def doSave(id: Option[ID], item: AnyRef) = {
    val newId = id.getOrElse {
      nextId += 1
      nextId
    }
    buffer += IdField.transformer[T](item.asInstanceOf[T])(Some(newId));
    newId
  }

  override protected def doDelete(uri: UriPath) {
    findAll(uri).foreach(entity => buffer -= entity)
  }
}