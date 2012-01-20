package com.github.scala.android.crud

import action.{InitializedContextVar, ContextVars, ContextWithVars}
import com.github.triangle.Logging
import common.Common
import java.util.NoSuchElementException
import collection.mutable
import persistence.EntityType
import java.util.concurrent.CopyOnWriteArraySet
import collection.JavaConversions._

/** An Application that works with [[com.github.scala.android.crud.CrudType]]s.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 4:50 PM
 */

trait CrudApplication extends Logging {
  def logTag = Common.tryToEvaluate(nameId).getOrElse(Common.logTag)

  trace("Instantiated CrudApplication: " + this)

  def name: String

  /** The version of the data such as a database.  This must be increased when new tables or columns need to be added, etc. */
  def dataVersion: Int

  //this will be used for programmatic uses such as a database name
  lazy val nameId: String = name.replace(" ", "_").toLowerCase

  def classNamePrefix: String = getClass.getSimpleName.replace("$", "").stripSuffix("Application")
  def packageName: String = getClass.getPackage.getName

  /** All entities in the application, in priority order of most interesting first. */
  def allCrudTypes: List[CrudType]
  def allEntityTypes: List[EntityType] = allCrudTypes.map(_.entityType)

  def crudType(entityType: EntityType): CrudType =
    allCrudTypes.find(_.entityType == entityType).getOrElse(throw new NoSuchElementException(entityType + " not found"))
}

/** A listener for when a CrudContext is being destroyed and resources should be released. */
trait DestroyContextListener {
  def onDestroyContext()
}

/** A context which can store data for the duration of a single Activity.
  * @author Eric Pabst (epabst@gmail.com)
  */

case class CrudContext(context: ContextWithVars, application: CrudApplication) {
  def vars: ContextVars = context

  def openEntityPersistence(entityType: EntityType): CrudPersistence =
    application.crudType(entityType).openEntityPersistence(this)

  /** This is final so that it will call the similar method even when mocking, making mocking easier when testing. */
  final def withEntityPersistence[T](entityType: EntityType)(f: CrudPersistence => T): T =
    withEntityPersistence_uncurried(entityType, f)

  /** This is useful for unit testing because it is much easier to mock than its counterpart. */
  def withEntityPersistence_uncurried[T](entityType: EntityType, f: CrudPersistence => T): T =
    application.crudType(entityType).withEntityPersistence(this)(f)

  def addOnRefreshListener(listener: OnRefreshListener, context: ContextVars) {
    OnRefreshListeners.get(context) += listener
  }

  def onRefresh(context: ContextVars) {
    OnRefreshListeners.get(context).foreach(_.onRefresh())
  }
}

/** Listeners that will listen to any EntityPersistence that is opened. */
object OnRefreshListeners extends InitializedContextVar[mutable.Set[OnRefreshListener]](new CopyOnWriteArraySet[OnRefreshListener]())

trait OnRefreshListener {
  def onRefresh()
}
