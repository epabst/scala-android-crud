package com.github.scala.android.crud

import action._
import common.{UriPath, Timing}
import Operation._
import android.app.Activity
import com.github.triangle._
import common.PlatformTypes._
import persistence.{EntityTypePersistedInfo, CursorStream, EntityType, PersistenceListener}
import PortableField.toSome
import view.AndroidResourceAnalyzer._
import java.lang.IllegalStateException
import android.view.View
import android.content.Context
import android.database.Cursor
import android.widget._
import java.util.concurrent.CopyOnWriteArraySet
import scala.collection.JavaConversions._
import collection.mutable
import view.{AdapterCachingStateListener, AdapterCaching, EntityAdapter}

/** An entity configuration that provides all custom information needed to
  * implement CRUD on the entity.  This shouldn't depend on the platform (e.g. android).
  * @author Eric Pabst (epabst@gmail.com)
  */
class CrudType(val entityType: EntityType, val persistenceFactory: PersistenceFactory) extends Timing with Logging { self =>
  protected def logTag = entityType.logTag

  trace("Instantiated CrudType: " + this)

  def entityName = entityType.entityName
  lazy val entityNameLayoutPrefix = NamingConventions.toLayoutPrefix(entityName)

  def rLayoutClasses: Seq[Class[_]] = detectRLayoutClasses(this.getClass)
  private lazy val rLayoutClassesVal = rLayoutClasses
  def rStringClasses: Seq[Class[_]] = detectRStringClasses(this.getClass)
  private lazy val rStringClassesVal = rStringClasses

  protected def getLayoutKey(layoutName: String): LayoutKey =
    findResourceIdWithName(rLayoutClassesVal, layoutName).getOrElse {
      rLayoutClassesVal.foreach(layoutClass => logError("Contents of " + layoutClass + " are " + layoutClass.getFields.mkString(", ")))
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

  /** This uses isDeletable because it assumes that if it can be deleted, it can be added as well.
    * @see [[com.github.scala.android.crud.CrudType.isDeletable]].
    */
  lazy val isAddable: Boolean = isDeletable

  /** @see [[com.github.scala.android.crud.PersistenceFactory.canDelete]]. */
  final lazy val isDeletable: Boolean = persistenceFactory.canDelete

  /** @see [[com.github.scala.android.crud.PersistenceFactory.canSave]]. */
  final lazy val isUpdateable: Boolean = persistenceFactory.canSave

  lazy val entityTypePersistedInfo = EntityTypePersistedInfo(entityType)

  protected def getStringKey(stringName: String): SKey =
    findResourceIdWithName(rStringClassesVal, stringName).getOrElse {
      rStringClassesVal.foreach(rStringClass => logError("Contents of " + rStringClass + " are " + rStringClass.getFields.mkString(", ")))
      throw new IllegalStateException("R.string." + stringName + " not found.  You may want to run the CrudUIGenerator.generateLayouts." +
              rStringClassesVal.mkString("(string classes: ", ",", ")"))
    }

  def listItemsString: Option[SKey] = findResourceIdWithName(rStringClassesVal, entityNameLayoutPrefix + "_list")
  def addItemString: SKey = getStringKey("add_" + entityNameLayoutPrefix)
  def editItemString: SKey = getStringKey("edit_" + entityNameLayoutPrefix)
  def deleteItemString: SKey = res.R.string.delete_item
  def cancelItemString: SKey = android.R.string.cancel

  lazy val parentFields: List[ParentField] = entityType.deepCollect {
    case parentField: ParentField => parentField
  }

  def parentEntityTypes(application: CrudApplication): List[EntityType] = parentFields.map(_.entityType)

  def parentEntities(application: CrudApplication): List[CrudType] = parentFields.map(_.entityType).map(application.crudType(_))

  def childEntityTypes(application: CrudApplication): List[EntityType] = childEntities(application).map(_.entityType)

  /** The list of entities that refer to this one.
    * Those entities should have a ParentField (or foreignKey) in their fields list.
    */
  def childEntities(application: CrudApplication): List[CrudType] = {
    trace("childEntities: allCrudTypes=" + application.allCrudTypes + " self=" + self)
    application.allCrudTypes.filter { entity =>
      val parentEntityTypes = entity.parentEntityTypes(application)
      trace("childEntities: parents of " + entity.entityType + " are " + parentEntityTypes)
      parentEntityTypes.contains(self.entityType)
    }
  }

  /** Gets the action to display a UI for a user to fill in data for creating an entity.
    * The target Activity should copy Unit into the UI using entityType.copy to populate defaults.
    */
  @deprecated("use CrudApplication.actionToCreate")
  lazy val createAction: Option[Action] =
    if (isAddable)
      Some(Action(Command(android.R.drawable.ic_menu_add, addItemString),
        new StartEntityActivityOperation(entityType.entityName, CreateActionName, activityClass)))
    else
      None

  /** Gets the action to display the list that matches the criteria copied from criteriaSource using entityType.copy. */
  @deprecated("use CrudApplication.actionToList.get")
  lazy val listAction = Action(Command(None, listItemsString), new StartEntityActivityOperation(entityType.entityName, ListActionName, listActivityClass))

  protected def entityOperation(action: String, activityClass: Class[_ <: Activity]) =
    new StartEntityIdActivityOperation(entityType.entityName, action, activityClass)

  /** Gets the action to display the entity given the id in the UriPath. */
  @deprecated("use CrudApplication.actionToDisplay.get")
  lazy val displayAction = Action(Command(None, None), entityOperation(DisplayActionName, activityClass))

  /** Gets the action to display a UI for a user to edit data for an entity given its id in the UriPath. */
  @deprecated("use CrudApplication.actionToUpdate")
  lazy val updateAction: Option[Action] =
    if (isUpdateable) Some(Action(Command(android.R.drawable.ic_menu_edit, editItemString), entityOperation(UpdateActionName, activityClass)))
    else None

  @deprecated("use CrudApplication.actionToDelete")
  lazy val deleteAction: Option[Action] =
    if (isDeletable) {
      Some(Action(Command(android.R.drawable.ic_menu_delete, deleteItemString), new Operation {
        def invoke(uri: UriPath, activity: ActivityWithVars) {
          activity match {
            case crudActivity: BaseCrudActivity => startDelete(uri, crudActivity)
          }
        }
      }))
    } else None


  def listActivityClass: Class[_ <: CrudListActivity] = classOf[CrudListActivity]
  def activityClass: Class[_ <: CrudActivity] = classOf[CrudActivity]

  def copyFromPersistedEntity(uriPathWithId: UriPath, crudContext: CrudContext): Option[PortableValue] = {
    val contextItems = List(uriPathWithId, crudContext, PortableField.UseDefaults)
    withEntityPersistence(crudContext)(_.find(uriPathWithId).map { readable =>
      debug("Copying " + entityType.entityName + "#" + entityType.IdField(readable) + " to " + this)
      entityType.copyFromItem(readable +: contextItems)
    })
  }

  /** Returns true if the URI is worth calling EntityPersistence.find to try to get an entity instance. */
  def maySpecifyEntityInstance(uri: UriPath): Boolean = persistenceFactory.maySpecifyEntityInstance(entityType, uri)

  /** Gets the actions that a user can perform from a list of the entities.
    * May be overridden to modify the list of actions.
    */
  @deprecated("use CrudApplication.actionsForList")
  def getListActions(application: CrudApplication): List[Action] =
    getReadOnlyListActions(application) ::: createAction.toList

  protected def getReadOnlyListActions(application: CrudApplication): List[Action] = {
    val thisEntity = this;
    (parentFields match {
      //exactly one parent w/o a display page
      case parentField :: Nil if !application.crudType(parentField.entityType).hasDisplayPage => {
        val parentCrudType = application.crudType(parentField.entityType)
        //the parent's updateAction should be shown since clicking on the parent entity brought the user
        //to the list of child entities instead of to a display page for the parent entity.
        parentCrudType.updateAction.toList ::: parentCrudType.childEntities(application).filter(_ != thisEntity).map(_.listAction)
      }
      case _ => Nil
    })
  }

  /** Gets the actions that a user can perform from a specific entity instance.
    * The first one is the one that will be used when the item is clicked on.
    * May be overridden to modify the list of actions.
    */
  @deprecated("use CrudApplication.actionsForEntity")
  def getEntityActions(application: CrudApplication): List[Action] =
    getReadOnlyEntityActions(application) ::: updateAction.toList ::: deleteAction.toList

  protected def getReadOnlyEntityActions(application: CrudApplication): List[Action] =
    displayLayout.map(_ => displayAction).toList ::: childEntities(application).map(_.listAction)

  /** Listeners that will listen to any EntityPersistence that is opened. */
  val persistenceListeners = new InitializedContextVar[mutable.Set[PersistenceListener]](new CopyOnWriteArraySet[PersistenceListener]())

  def addPersistenceListener(listener: PersistenceListener, context: ContextVars) {
    persistenceListeners.get(context) += listener
  }

  def openEntityPersistence(crudContext: CrudContext): CrudPersistence = {
    val persistence = createEntityPersistence(crudContext)
    persistenceListeners.get(crudContext.vars).foreach(persistence.addListener(_))
    persistence
  }

  /** Instantiates a data buffer which can be saved by EntityPersistence.
    * The fields must support copying into this object.
    */
  def newWritable = persistenceFactory.newWritable

  protected def createEntityPersistence(crudContext: CrudContext) = persistenceFactory.createEntityPersistence(entityType, crudContext)

  final def withEntityPersistence[T](crudContext: CrudContext)(f: CrudPersistence => T): T = {
    val persistence = openEntityPersistence(crudContext)
    try f(persistence)
    finally persistence.close()
  }

  final def setListAdapterUsingUri(crudContext: CrudContext, activity: CrudListActivity) {
    setListAdapter(activity.getListView, entityType, activity.currentUriPath, crudContext, activity.contextItems, activity, self.rowLayout)
  }

  private def createAdapter[A <: Adapter](persistence: CrudPersistence, uriPath: UriPath, entityType: EntityType, crudContext: CrudContext, contextItems: scala.List[AnyRef], activity: Activity, itemLayout: LayoutKey, adapterView: AdapterView[A]): AdapterCaching = {
    val findAllResult = persistence.findAll(uriPath)
    findAllResult match {
      case CursorStream(cursor, _) =>
        activity.startManagingCursor(cursor)
        addPersistenceListener(new PersistenceListener {
          def onSave(id: ID) {
            cursor.requery()
          }
          def onDelete(uri: UriPath) {
            cursor.requery()
          }
        }, crudContext.vars)
        new ResourceCursorAdapter(activity, itemLayout, cursor) with AdapterCaching {
          def entityType = self.entityType

          def bindView(view: View, context: Context, cursor: Cursor) {
            val row = entityTypePersistedInfo.copyRowToMap(cursor)
            bindViewFromCacheOrItems(view, row, contextItems, cursor.getPosition, adapterView)
          }
        }
      case _ => new EntityAdapter(entityType, findAllResult, itemLayout, contextItems, activity.getLayoutInflater)
    }
  }

  private def setListAdapter[A <: Adapter](adapterView: AdapterView[A], persistence: CrudPersistence, uriPath: UriPath, entityType: EntityType, crudContext: CrudContext, contextItems: scala.List[AnyRef], activity: Activity, itemLayout: LayoutKey) {
    addPersistenceListener(new PersistenceListener {
      def onSave(id: ID) {
        AdapterCaching.clearCache(adapterView, "save")
      }

      def onDelete(uri: UriPath) {
        trace("Clearing cache in " + adapterView + " of " + entityType + " due to delete")
        AdapterCaching.clearCache(adapterView, "delete")
      }
    }, crudContext.vars)
    def callCreateAdapter(): A = {
      createAdapter(persistence, uriPath, entityType, crudContext, contextItems, activity, itemLayout, adapterView).asInstanceOf[A]
    }
    val adapter = callCreateAdapter()
    adapterView.setAdapter(adapter)
    crudContext.addCachedStateListener(new AdapterCachingStateListener(adapterView, entityType, adapterFactory = callCreateAdapter()))
  }

  def setListAdapter[A <: Adapter](adapterView: AdapterView[A], entityType: EntityType, uriPath: UriPath, crudContext: CrudContext, contextItems: scala.List[AnyRef], activity: Activity, itemLayout: LayoutKey) {
    val persistence = crudContext.openEntityPersistence(entityType)
    crudContext.vars.addListener(new DestroyContextListener {
      def onDestroyContext() {
        persistence.close()
      }
    })
    setListAdapter(adapterView, persistence, uriPath, entityType, crudContext, contextItems, activity, itemLayout)
  }

  private[crud] def undoableDelete(uri: UriPath)(persistence: CrudPersistence) {
    persistence.find(uri).foreach { readable =>
      val id = entityType.IdField.getter(readable)
      val writable = entityType.copyAndTransform(readable, newWritable)
      persistence.delete(uri)
      val undoDeleteOperation = new PersistenceOperation(entityType, persistence.crudContext.application) {
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

/** An undo of an operation.  The operation should have already completed, but it can be undone or accepted.
  * @param undoAction  An Action that reverses the operation.
  * @param closeOperation  An operation that releases any resources, and is guaranteed to be called.
  *           For example, deleting related entities if undo was not called.
  */
case class Undoable(undoAction: Action, closeOperation: Option[Operation] = None)
