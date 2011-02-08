package geeks.crud.persistence

import java.lang.UnsupportedOperationException

/**
 * todo A ... 
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 6:49 AM
 */

trait IdPk {
  def id: Long = throw new UnsupportedOperationException("todo implement")
}