package com.github.scala_android.crud

import action._
import action.UriPath
import android.view.View
import com.github.triangle.JavaUtil._
import common.{Timing, PlatformTypes}
import android.database.DataSetObserver
import android.widget.{ListAdapter, BaseAdapter}
import persistence.{PersistenceListener, IdPk, EntityPersistence}
import Action._
import android.app.{Activity, ListActivity}
import com.github.scala_android.crud.view.ViewField._
import com.github.triangle._
import PortableField.toSome
/**
 * An entity configuration that provides all custom information needed to
 * implement CRUD on the entity.  This shouldn't depend on the platform (e.g. android).
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:24 PM
 */
trait CrudType extends FieldList with PlatformTypes with Logging with Timing {
  trace("Instantiated CrudType: " + this)

  //this is the type used for internationalized strings
  def entityName: String

  /**
   * These are all of the entity's fields, which includes IdPk.idField and the valueFields.
   */
  final lazy val fields: List[BaseField] = idField +: valueFields

  lazy val uriPathId = uriIdField(entityName)

  def toUri(id: ID) = UriPath(entityName, id.toString)

  def idField = IdPk.idField

  /**
   * The fields other than the primary key.
   */
  def valueFields: List[BaseField]

  def headerLayout: LayoutKey
  def listLayout: LayoutKey
  def rowLayout: LayoutKey
  def displayLayout: Option[LayoutKey]
  def entryLayout: LayoutKey

  final def hasDisplayPage = displayLayout.isDefined

  lazy val unitPortableValue = copyFrom(Unit)

  private val persistenceVarForListAdapter = new ContextVar[CrudPersistence]

  def listItemsString: Option[SKey] = None
  def addItemString: SKey
  def editItemString: SKey
  def deleteItemString: SKey = res.R.string.delete_item
  def cancelItemString: SKey

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
  lazy val createAction = new StartEntityActivityAction(entityName, CreateActionName,
    android.R.drawable.ic_menu_add, addItemString, activityClass)

  /**
   * Gets the action to display the list that matches the criteria copied from criteriaSource using entityType.copy.
   */
  lazy val listAction = new StartEntityActivityAction(entityName, ListActionName,
    None, listItemsString, listActivityClass)

  protected def entityAction(action: String, icon: Option[PlatformTypes#ImgKey], title: Option[PlatformTypes#SKey],
                             activityClass: Class[_ <: Activity]) =
    new StartEntityIdActivityAction(entityName, action, icon, title, activityClass)

  /**
   * Gets the action to display the entity given the id in the UriPath.
   */
  lazy val displayAction = entityAction(DisplayActionName, None, None, activityClass)

  /**
   * Gets the action to display a UI for a user to edit data for an entity given its id in the UriPath.
   */
  lazy val updateAction = entityAction(UpdateActionName, android.R.drawable.ic_menu_edit, editItemString, activityClass)

  lazy val deleteAction = new RunnableAction(android.R.drawable.ic_menu_delete, deleteItemString) {
    def invoke(uri: UriPath, activity: Activity) {
      activity match {
        case crudActivity: BaseCrudActivity =>
          startDelete(uri.findId(entityName).get, crudActivity)
      }
    }
  }


  def listActivityClass: Class[_ <: CrudListActivity]
  def activityClass: Class[_ <: CrudActivity]

  def findId(uri: UriPath): Option[ID] = uri.findId(entityName)

  def copyFromPersistedEntity(uriPathWithId: UriPath, crudContext: CrudContext): Option[PortableValue] = {
    findId(uriPathWithId).flatMap { id =>
      val contextItems = List(uriPathWithId, crudContext, Unit)
      withEntityPersistence(crudContext, _.find(id).map { readable =>
        debug("Copying " + entityName + "#" + id + " to " + this)
        copyFromItem(readable :: contextItems)
      })
    }
  }

  override def toString() = entityName

  /**
   * Gets the actions that a user can perform from a list of the entities.
   * May be overridden to modify the list of actions.
   */
  def getListActions(application: CrudApplication): List[Action] =
    getReadOnlyListActions(application) ::: createAction :: Nil

  protected def getReadOnlyListActions(application: CrudApplication): List[Action] = {
    val thisEntity = this;
    (parentFields match {
      //exactly one parent w/o a display page
      case parentField :: Nil if !parentField.entityType.hasDisplayPage => {
        val parentEntity = parentField.entityType
        parentEntity.updateAction :: parentEntity.childEntities(application).filter(_ != thisEntity).map(_.listAction)
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
    getReadOnlyEntityActions(application) ::: List(updateAction, deleteAction)

  protected def getReadOnlyEntityActions(application: CrudApplication): List[Action] =
    displayLayout.map(_ => displayAction).toList ::: childEntities(application).map(_.listAction)

  /**
   * Instantiates a data buffer which can be saved by EntityPersistence.
   * The fields must support copying into this object.
   */
  def newWritable: AnyRef

  def openEntityPersistence(crudContext: CrudContext): CrudPersistence

  final def withEntityPersistence[T](crudContext: CrudContext, f: CrudPersistence => T): T = {
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

    def cacheClearingObserver(activity: ListActivity) = new DataSetObserver {
      override def onInvalidated() {
        trace("Clearing ListView cache in " + activity + " since DataSet was invalidated")
        activity.runOnUiThread { activity.getListView.setTag(null) }
        super.onInvalidated()
      }
    }

    protected def bindViewFromCacheOrItems(view: View, itemsToCopyAtPosition: => List[AnyRef], position: Long, activity: ListActivity) {
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

  final def setListAdapter(crudContext: CrudContext, activity: CrudListActivity) {
    val persistence = openEntityPersistence(crudContext)
    persistenceVarForListAdapter.set(crudContext, persistence)
    setListAdapter(persistence, crudContext, activity)
    val listAdapter = activity.getListAdapter
    persistence.addListener(new PersistenceListener {
      def onSave(id: ID) { refreshAfterDataChanged(listAdapter) }

      def onDelete(ids: Seq[ID]) { refreshAfterDataChanged(listAdapter) }
    })
  }

  def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: CrudListActivity) {
    val findAllResult = persistence.findAll(activity.currentUriPath)
    setListAdapter(findAllResult, List(activity.currentUriPath, crudContext, Unit), activity)
  }

  def setListAdapter(findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: ListActivity)

  def refreshAfterDataChanged(listAdapter: ListAdapter)

  def destroyContextVars(crudContext: CrudContext) {
    persistenceVarForListAdapter.clear(crudContext).map { persistence =>
      persistence.close()
    }
  }

  private[crud] def undoableDelete(id: ID, activity: BaseCrudActivity)(persistence: EntityPersistence) {
    persistence.find(id).map { readable =>
      val writable = transform(newWritable, readable)
      persistence.delete(List(id))
      activity.addUndoableDelete(this, new Undoable[ID] {
        def undo(): ID = {
          persistence.save(None, writable)
        }

        def close() {
          //todo delete childEntities(application) recursively
        }
      })
    }
  }

  /**
   * Delete an entity by ID with an undo option.  It can be overridden to do a confirmation box if desired.
   */
  def startDelete(id: ID, activity: BaseCrudActivity) {
    withEntityPersistence(activity.crudContext, undoableDelete(id, activity))
  }
}

/**
 * A trait for stubbing out the UI methods of CrudType for use when the entity will
 * never be used with the UI.
 */
trait HiddenEntityType extends CrudType {
  def headerLayout: LayoutKey = throw new UnsupportedOperationException
  def listLayout: LayoutKey = throw new UnsupportedOperationException
  def rowLayout: LayoutKey = throw new UnsupportedOperationException
  def displayLayout: Option[LayoutKey] = throw new UnsupportedOperationException
  def entryLayout: LayoutKey = throw new UnsupportedOperationException
  def addItemString: SKey = throw new UnsupportedOperationException
  def editItemString: SKey = throw new UnsupportedOperationException
  def cancelItemString: SKey = throw new UnsupportedOperationException
  def activityClass = throw new UnsupportedOperationException
  def listActivityClass = throw new UnsupportedOperationException
}

/**
 * An undoable command.  The command should have already completed, but it can be undone or accepted.
 */
trait Undoable[T] {
  /** Reverses the action. */
  def undo(): T

  /** Releases any resources, and is guaranteed to be called.  For example, deleting related entities if undo was not called. */
  def close()
}
