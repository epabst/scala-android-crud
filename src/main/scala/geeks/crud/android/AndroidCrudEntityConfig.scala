package geeks.crud.android

import util.matching.Regex
import geeks.crud.{EntityPersistence, UIActionFactory, CrudEntityConfig}
import android.content.Context

/**
 * A base type for CrudEntityConfig on the android platform.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:31 PM
 */

trait AndroidCrudEntityConfig[ID,Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef] extends CrudEntityConfig[ID,Int,Int] {
  def getEntityPersistence(context: Context): AndroidEntityPersistence[ID,Q,L,R,W]
}

/** A Regex that can match a URI string that ends with a numeric id */
object IdUri extends Regex("(.*?)/?([0-9]+)", "prefix", "id")

/** A Regex that can match a URI string that ends with a string */
object NameUri extends Regex("(.*?)/?([^/]+)", "prefix", "name")

trait AndroidUIActionFactory[ID] extends UIActionFactory[ID,Int]
