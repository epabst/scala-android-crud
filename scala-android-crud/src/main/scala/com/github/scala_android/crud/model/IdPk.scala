package com.github.scala_android.crud.model

import com.github.triangle.PortableField._
import com.github.scala_android.crud.{CursorField, PlatformTypes}

/**
 * A trait with a primary key
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 6:49 AM
 */

trait IdPk extends PlatformTypes {
  var id: Option[ID] = None
}

object IdPk extends PlatformTypes {
  val idField = fieldOpt[IdPk,ID](_.id, _.id_=) + CursorField.persistedId
}