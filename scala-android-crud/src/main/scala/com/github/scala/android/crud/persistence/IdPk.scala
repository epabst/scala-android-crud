package com.github.scala.android.crud.persistence

import com.github.scala.android.crud.common.PlatformTypes

/**
 * A trait with a primary key
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 6:49 AM
 */

trait IdPk extends PlatformTypes {
  var id: Option[ID] = None
}
