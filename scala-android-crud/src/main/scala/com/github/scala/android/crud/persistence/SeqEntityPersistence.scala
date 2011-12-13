package com.github.scala.android.crud.persistence

import scala.collection.mutable
import com.github.scala.android.crud.common.UriPath
import com.github.triangle.{Setter, Getter, Field}
import com.github.scala.android.crud.common.PlatformTypes._

/**
 * EntityPersistence for a simple generated Seq.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 5:05 PM
 */

trait SeqEntityPersistence[T <: AnyRef] extends EntityPersistence {
  def newWritable: T
}

trait ReadOnlyPersistence extends EntityPersistence {
  def newWritable = throw new UnsupportedOperationException("write not supported")

  def doSave(id: Option[ID], data: AnyRef): ID = throw new UnsupportedOperationException("write not supported")

  def doDelete(uri: UriPath) { throw new UnsupportedOperationException("delete not supported") }

  def close() {}
}

abstract class ListBufferEntityPersistence[T <: AnyRef](newWritableFunction: => T) extends SeqEntityPersistence[T] {
  private object IdField extends Field[ID](Getter[IdPk,ID](_.id).withTransformer(e => e.id(_)) +
      Setter((e: MutableIdPk) => e.id = _) + CursorField.PersistedId)
  val buffer = mutable.ListBuffer[T]()

  var nextId = 10000L

  //todo only return the one that matches the ID in the uri, if present
  //def findAll(uri: UriPath) = buffer.toList.filter(item => uri.segments.containsSlice(toUri(IdField(item)).segments))
  def findAll(uri: UriPath) = buffer.toList

  def newWritable = newWritableFunction

  def doSave(id: Option[ID], item: AnyRef) = {
    val newId = id.getOrElse {
      nextId += 1
      nextId
    }
    buffer += IdField.transformer[T](item.asInstanceOf[T])(Some(newId));
    newId
  }

  def doDelete(uri: UriPath) {
    findAll(uri).foreach(entity => buffer -= entity)
  }

  def close() {}
}
