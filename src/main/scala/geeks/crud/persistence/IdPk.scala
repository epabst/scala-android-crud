package geeks.crud.persistence

/**
 * A trait with a primary key
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 6:49 AM
 */

trait IdPk {
  type ID = Long
  var id: ID = _
}