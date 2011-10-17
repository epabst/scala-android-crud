package com.github.scala.android.crud.persistence

import com.github.triangle.PortableField._
import com.github.scala.android.crud.common.PlatformTypes
import com.github.triangle.Field

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
  object IdField extends Field[ID](field[IdPk,ID](_.id, e => e.id = _) + CursorField.PersistedId)
}