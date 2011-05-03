package com.github.scala_android.crud.model

import com.github.scala_android.crud.PlatformTypes
import com.github.triangle.PortableField._

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
  val idField = field[IdPk,ID](_.id, pk => id => pk.id = Some(id), pk => pk.id = None)
}