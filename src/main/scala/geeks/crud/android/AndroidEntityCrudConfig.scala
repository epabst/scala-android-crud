package geeks.crud.android

import geeks.crud.EntityCrudConfig

/**
 * A base type for EntityCrudConfig on the android platform.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:31 PM
 */

trait AndroidEntityCrudConfig extends EntityCrudConfig[Int,Int] {
  type ID = Long
}