package com.github.scala.android.crud

import action._
import android.view.View
import com.github.triangle.JavaUtil._
import android.widget.{ListAdapter, BaseAdapter}
import common.{UriPath, Timing}
import Operation._
import android.app.{Activity, ListActivity}
import com.github.triangle._
import common.PlatformTypes._
import persistence.{EntityType, CursorField, PersistenceListener}
import PortableField.toSome
import view.AndroidResourceAnalyzer._
import java.lang.IllegalStateException

/**
 * An entity configuration that provides all custom information needed to
 * implement CRUD on the entity.  This shouldn't depend on the platform (e.g. android).
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:24 PM
 */
abstract class CrudType(persistenceFactory: PersistenceFactory) extends EntityType with Timing {
  trace("Instantiated CrudType: " + this)

  lazy val entityNameLayoutPrefix = NamingConventions.toLayoutPrefix(entityName)

  def rIdClasses: Seq[Class[_]] = detectRIdClasses(this.getClass)
  def rLayoutClasses: Seq[Class[_]] = detectRLayoutClasses(this.getClass)
  private lazy val rLayoutClassesVal = rLayoutClasses
  def rStringClasses: Seq[Class[_]] = detectRStringClasses(this.getClass)
  private lazy val rStringClassesVal = rStringClasses

  protected def getLayoutKey(layoutName: String): LayoutKey =
    findResourceIdWithName(rLayoutClassesVal, layoutName).getOrElse {
      rLayoutClassesVal.foreach(layoutClass => error("Contents of " + layoutClass + " are " + layoutClass.getFields.mkString(", ")))
      throw new IllegalStateException("R.layout." + layoutName + " not found.  You may want to run the CrudUIGenerator.generateLayouts." +
              rLayoutClassesVal.mkString("(layout classes: ", ",", ")"))
    }

  lazy val headerLayout: LayoutKey = getLayoutKey(entityNameLayoutPrefix + "_header")
  lazy val listLayout: LayoutKey =
    findResourceIdWithName(rLayoutClassesVal, entityNameLayoutPrefix + "_list").getOrElse(getLayoutKey("entity_list"))
  lazy val rowLayout: LayoutKey = getLayoutKey(entityNameLayoutPrefix + "_row")
  lazy val displayLayout: Option[LayoutKey] = findResourceIdWithName(rLayoutClassesVal, entityNameLayoutPrefix + "_display")
  lazy val entryLayout: LayoutKey = getLayoutKey(entityNameLayoutPrefix + "_entry")

  final def hasDisplayPage = displayLayout.isDefined
  lazy val isUpdateable: Boolean = !CursorField.updateablePersistedFields(this, rIdClasses).isEmpty

  private val persistenceVarForListAdapter = new ContextVar[CrudPersistence]

  protected def getStringKey(stringName: String): SKey =
    findResourceIdWithName(rStringClassesVal, stringName).getOrElse {
      rStringClassesVal.foreach(rStringClass => error("Contents of " + rStringClass + " are " + rStringClass.getFields.mkString(", ")))
      throw new IllegalStateException("R.string." + stringName + " not found.  You may want to run the CrudUIGenerator.generateLayouts." +
              rStringClassesVal.mkString("(string classes: ", ",", ")"))
    }

  def listItemsString: Option[SKey] = findResourceIdWithName(rStringClassesVal, entityNameLayoutPrefix + "_list")
  def addItemString: SKey = getStringKey("add_" + entityNameLayoutPrefix)
  def editItemString: SKey = getStringKey("edit_" + entityNameLayoutPrefix)
  def deleteItemString: SKey = res.R.string.delete_item
  def cancelItemString: SKey = android.R.string.cancel

  lazy val parentFields: List[ParentField] = deepCollect {
    case parentField: ParentField => parentField
  }

  lazy val parentEntities: List[CrudType] = parentFields.map(_.entityType)

  /**
   * The list of entities that refer to this one.
   * Those entities should have a ParentField (or foreignKey) in their fields list.
   */
  def childEntities(application: CrudApplication): List[CrudType] = {
    val self = this
    trace("childEntities: allEntities=" + application.allEntities + " self=" + self)
    application.allEntities.filter { entity =>
      val entityParents = entity.parentEntities
      trace("childEntities: parents of " + entity + " are " + entityParents)
      entityParents.contains(self)
    }
  }

  /**
   * Gets the action to display a UI for a user to fill in data for creating an entity.
   * The target Activity should copy Unit into the UI using entityType.copy to populate defaults.
   */
  lazy val createAction: Option[Action] =
    if (isUpdateable)
      Some(Action(Command(android.R.drawable.ic_menu_add, addItemString), new StartEntityActivityOperation(entityName, CreateActionName, activityClass)))
    else
      None

  /**
   * Gets the action to display the list that matches the criteria copied from criteriaSource using entityType.copy.
   */
  lazy val listAction = Action(Command(None, listItemsString), new StartEntityActivityOperation(entityName, ListActionName, listActivityClass))

  protected def entityOperation(action: String, activityClass: Class[_ <: Activity]) =
    new StartEntityIdActivityOperation(entityName, action, activityClass)

  /**
   * Gets the action to display the entity given the id in the UriPath.
   */
  lazy val displayAction = Action(Command(None, None), entityOperation(DisplayActionName, activityClass))

  /**
   * Gets the action to display a UI for a user to edit data for an entity given its id in the UriPath.
   */
  lazy val updateAction: Option[Action] =
    if (isUpdateable) Some(Action(Command(android.R.drawable.ic_menu_edit, editItemString), entityOperation(UpdateActionName, activityClass)))
    else None

  lazy val deleteAction: Option[Action] =
    if (isUpdateable) {
      Some(Action(Command(android.R.drawable.ic_menu_delete, deleteItemString), new Operation {
        def invoke(uri: UriPath, activity: ActivityWithVars) {
          activity match {
            case crudActivity: BaseCrudActivity => startDelete(uri, crudActivity)
          }
        }
      }))
    } else None


  def listActivityClass: Class[_ <: CrudListActivity]
  def activityClass: Class[_ <: CrudActivity]

  def copyFromPersistedEntity(uriPathWithId: UriPath, crudContext: CrudContext): Option[PortableValue] = {
    val contextItems = List(uriPathWithId, crudContext, Unit)
    withEntityPersistence(crudContext)(_.find(uriPathWithId).map { readable =>
      debug("Copying " + entityName + "#" + IdField(readable) + " to " + this)
      copyFromItem(readable +: contextItems)
    })
  }

  /**
   * Gets the actions that a user can perform from a list of the entities.
   * May be overridden to modify the list of actions.
   */
  def getListActions(application: CrudApplication): List[Action] =
    getReadOnlyListActions(application) ::: createAction.toList

  protected def getReadOnlyListActions(application: CrudApplication): List[Action] = {
    val thisEntity = this;
    (parentFields match {
      //exactly one parent w/o a display page
      case parentField :: Nil if !parentField.entityType.hasDisplayPage => {
        val parentEntity = parentField.entityType
        //the parent's updateAction should be shown since clicking on the parent entity brought the user
        //to the list of child entities instead of to a display page for the parent entity.
        parentEntity.updateAction.toList ::: parentEntity.childEntities(application).filter(_ != thisEntity).map(_.listAction)
      }
      case _ => Nil
    })
  }

  /**
   * Gets the actions that a user can perform from a specific entity instance.
   * The first one is the one that will be used when the item is clicked on.
   * May be overridden to modify the list of actions.
   */
  def getEntityActions(application: CrudApplication): List[Action] =
    getReadOnlyEntityActions(application) ::: updateAction.toList ::: deleteAction.toList

  protected def getReadOnlyEntityActions(application: CrudApplication): List[Action] =
    displayLayout.map(_ => displayAction).toList ::: childEntities(application).map(_.listAction)

  /** Listeners that will listen to any EntityPersistence that is opened. */
  val persistenceListeners = new ContextVar[Seq[PersistenceListener]]

  def addPersistenceListener(listener: PersistenceListener, context: ContextVars) {
    persistenceListeners.set(context, listener +: persistenceListeners.get(context).getOrElse(Nil))
  }

  def openEntityPersistence(crudContext: CrudContext): CrudPersistence = {
    val persistence = createEntityPersistence(crudContext)
    persistenceListeners.get(crudContext.vars).getOrElse(Nil).foreach(persistence.addListener(_))
    persistence
  }

  /**
   * Instantiates a data buffer which can be saved by EntityPersistence.
   * The fields must support copying into this object.
   */
  def newWritable = persistenceFactory.newWritable

  protected def createEntityPersistence(crudContext: CrudContext) = persistenceFactory.createEntityPersistence(this, crudContext)

  final def withEntityPersistence[T](crudContext: CrudContext)(f: CrudPersistence => T): T = {
    val persistence = openEntityPersistence(crudContext)
    try f(persistence)
    finally persistence.close()
  }

  trait AdapterCaching { self: BaseAdapter =>
    private def findCachedPortableValue(activity: ListActivity, position: Long): Option[PortableValue] =
      Option(activity.getListView.getTag.asInstanceOf[Map[Long, PortableValue]]).flatMap(_.get(position))

    private def cachePortableValue(activity: ListActivity, position: Long, portableValue: PortableValue) {
      val listView = activity.getListView
      val map = Option(listView.getTag.asInstanceOf[Map[Long,PortableValue]]).getOrElse(Map.empty[Long,PortableValue]) +
              (position -> portableValue)
      listView.setTag(map)
      trace("Added value at position " + position + " to the cache for " + activity)
    }

    def cacheClearingPersistenceListener(activity: ListActivity) = new PersistenceListener {
      def onSave(id: ID) {
        trace("Clearing ListView cache in " + activity + " since DataSet was invalidated")
        activity.runOnUiThread { activity.getListView.setTag(null) }
      }

      def onDelete(uri: UriPath) {
        trace("Clearing ListView cache in " + activity + " since DataSet was invalidated")
        activity.runOnUiThread { activity.getListView.setTag(null) }
      }
    }

    protected[crud] def bindViewFromCacheOrItems(view: View, itemsToCopyAtPosition: => List[AnyRef], position: Long, activity: ListActivity) {
      val cachedValue: Option[PortableValue] = findCachedPortableValue(activity, position)
      //set the cached or default values immediately instead of showing the column header names
      cachedValue match {
        case Some(portableValue) =>
          trace("cache hit for " + activity + " at position " + position + ": " + portableValue)
          portableValue.copyTo(view)
        case None =>
          trace("cache miss for " + activity + " at position " + position)
          unitPortableValue.copyTo(view)
      }
      if (cachedValue.isEmpty) {
        //copy immediately since in the case of a Cursor, it will be advanced to the next row quickly.
        val positionItems: List[AnyRef] = itemsToCopyAtPosition
        cachePortableValue(activity, position, unitPortableValue)
        future {
          val portableValue = copyFromItem(positionItems)
          activity.runOnUiThread {
            cachePortableValue(activity, position, portableValue)
            notifyDataSetChanged()
          }
        }
      }
    }
  }

  final def setListAdapterUsingUri(crudContext: CrudContext, activity: CrudListActivity) {
    val persistence = openEntityPersistence(crudContext)
    persistenceVarForListAdapter.set(crudContext.vars, persistence)
    setListAdapterUsingUri(persistence, crudContext, activity)
  }

  def setListAdapterUsingUri(persistence: CrudPersistence, crudContext: CrudContext, activity: CrudListActivity) {
    val findAllResult = persistence.findAll(activity.currentUriPath)
    setListAdapter(findAllResult, List(activity.currentUriPath, crudContext, Unit), activity)
  }

  def setListAdapter(findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {
    persistenceFactory.setListAdapter(this, findAllResult,  contextItems, activity)
  }

  def refreshAfterDataChanged(listAdapter: ListAdapter) {
    persistenceFactory.refreshAfterDataChanged(listAdapter)
  }

  def destroyContextVars(vars: ContextVars) {
    persistenceVarForListAdapter.clear(vars).map { persistence =>
      persistence.close()
    }
  }

  private[crud] def undoableDelete(uri: UriPath)(persistence: CrudPersistence) {
    persistence.find(uri).foreach { readable =>
      val id = idField.getter(readable)
      val writable = transform(newWritable, readable)
      persistence.delete(uri)
      val undoDeleteOperation = new PersistenceOperation(this, persistence.crudContext.application) {
        def invoke(uri: UriPath, persistence: CrudPersistence) {
          persistence.save(id, writable)
        }
      }
      //todo delete childEntities(application) recursively
      val context = persistence.crudContext.context
      context match {
        case activity: BaseCrudActivity =>
          activity.allowUndo(Undoable(Action(Command(None, Some(res.R.string.undo_delete)), undoDeleteOperation), None))
        case _ =>
      }
    }
  }

  /** Delete an entity by Uri with an undo option.  It can be overridden to do a confirmation box if desired. */
  def startDelete(uri: UriPath, activity: BaseCrudActivity) {
    withEntityPersistence(activity.crudContext)(undoableDelete(uri))
  }
}

abstract class PersistedCrudType(persistenceFactory: PersistenceFactory) extends CrudType(persistenceFactory)

/**
 * A trait for stubbing out the UI methods of CrudType for use when the entity will
 * never be used with the UI.
 */
trait HiddenEntityType extends CrudType {
  def activityClass: Class[_ <: CrudActivity] = throw new UnsupportedOperationException
  def listActivityClass: Class[_ <: CrudListActivity] = throw new UnsupportedOperationException
}

/** An undo of an operation.  The operation should have already completed, but it can be undone or accepted.
  * @param undoAction  An Action that reverses the operation.
  * @param closeAction  An action that releases any resources, and is guaranteed to be called.
  *           For example, deleting related entities if undo was not called.
  */
case class Undoable(undoAction: Action, closeOperation: Option[Operation] = None)
