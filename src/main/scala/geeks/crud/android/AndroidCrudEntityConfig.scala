package geeks.crud.android

import util.matching.Regex
import geeks.crud.{UIActionFactory, CrudEntityConfig}

/**
 * A base type for CrudEntityConfig on the android platform.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:31 PM
 */

trait AndroidCrudEntityConfig[ID] extends CrudEntityConfig[ID,Int,Int]

/** A Regex that can match a URI string that ends with a numeric id */
object IdUri extends Regex("(.*?)/?([0-9]+)", "prefix", "id")

/** A Regex that can match a URI string that ends with a string */
object NameUri extends Regex("(.*?)/?([^/]+)", "prefix", "name")

trait AndroidUIActionFactory[ID] extends UIActionFactory[ID,Int]
