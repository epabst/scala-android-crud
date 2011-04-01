package com.github.scala_android.crud.model

import com.github.scala_android.crud.PlatformTypes

/**
 * A trait with a primary key
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 6:49 AM
 */

trait IdPk extends PlatformTypes {
  var id: ID = _
}