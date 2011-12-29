package com.github.scala.android.crud.persistence

import com.github.scala.android.crud.common.PlatformTypes._

/** A trait with a primary key
  * @author Eric Pabst (epabst@gmail.com)
  */
trait IdPk {
  def id: Option[ID]

  def id(newId: Option[ID]): IdPk
}

trait MutableIdPk extends IdPk {
  var id: Option[ID] = None

  def id(newId: Option[ID]) = {
    id = newId
    this
  }
}
