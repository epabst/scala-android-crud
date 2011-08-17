package com.github.scala_android.crud

import android.net.Uri
import android.app.ListActivity
import android.view.View
import com.github.triangle.{PortableValue, FieldList, BaseField}
import com.github.triangle.JavaUtil._
import common.{Timing, PlatformTypes, Logging}
import android.database.DataSetObserver
import android.content.Intent
import persistence.{IdPk, EntityPersistence, CrudPersistence}
import android.widget.{AdapterView, BaseAdapter}
import android.graphics.Rect

/**
 * An entity configuration that provides all custom information needed to
 * implement CRUD on the entity.  This shouldn't depend on the platform (e.g. android).
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:24 PM
 */
trait CrudType extends FieldList with PlatformTypes with Logging with Timing {
  verbose("Instantiated CrudType: " + this)

  //this is the type used for internationalized strings
  def entityName: String

  /**
   * These are all of the entity's fields, which includes IdPk.idField and the valueFields.
   */
  final lazy val fields: List[BaseField] = IdPk.idField +: valueFields

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

  lazy val foreignKeys: List[ForeignKey] = deepCollect {
    case foreignKey: ForeignKey => foreignKey
  }

  lazy val parentEntities: List[CrudType] = foreignKeys.map(_.entityType)

  /**
   * The list of entities that refer to this one.
   * Those entities should have a foreignKey in their fields list, if persisted.
   */
  def childEntities(application: CrudApplication): List[CrudType] = {
    val self = this
    verbose("childEntities: allEntities=" + application.allEntities + " self=" + self)
    application.allEntities.filter { entity =>
      val entityParents = entity.parentEntities
      verbose("childEntities: parents of " + entity + " are " + entityParents)
      entityParents.contains(self)
    }
  }

  def displayChildEntityLists[T](actionFactory: UIActionFactory, idGetter: T => ID,
                                 childEntities: List[CrudType]): List[UIAction[T]] =
    childEntities.map(entity => actionFactory.adapt(actionFactory.displayList(entity), (value: T) =>
      Some(EntityUriSegment(entityName, idGetter(value).toString))))

  def listActivityClass: Class[_ <: CrudListActivity]
  def activityClass: Class[_ <: CrudActivity]

  def findId(uri: Uri): Option[ID] = new EntityUriSegment(entityName).findId(uri)

  def copyFromPersistedEntity(intentWithId: Intent, crudContext: CrudContext): Option[PortableValue] = {
    findId(intentWithId.getData).flatMap { id =>
      val contextItems = List(intentWithId, crudContext, Unit)
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
  def getListActions(actionFactory: UIActionFactory): List[UIAction[Unit]] = {
    getReadOnlyListActions(actionFactory) ::: actionFactory.startCreate(this) :: Nil
  }

  protected def getReadOnlyListActions(actionFactory: UIActionFactory): List[UIAction[Unit]] = {
    val thisEntity = this;
    (foreignKeys match {
      //exactly one parent w/o a display page
      case foreignKey :: Nil if !foreignKey.entityType.hasDisplayPage => {
        val parentEntity = foreignKey.entityType
        val getForeignKey = { _: Unit => foreignKey.apply(actionFactory.currentIntent) }
        actionFactory.adapt(actionFactory.startUpdate(parentEntity), getForeignKey) ::
                parentEntity.displayChildEntityLists(actionFactory, getForeignKey,
                  parentEntity.childEntities(actionFactory.application).filter(_ != thisEntity))
      }
      case _ => Nil
    })
  }

  /**
   * Gets the actions that a user can perform from a specific entity instance.
   * The first one is the one that will be used when the item is clicked on.
   * May be overridden to modify the list of actions.
   */
  def getEntityActions(actionFactory: UIActionFactory): List[UIAction[ID]] =
    getReadOnlyEntityActions(actionFactory) ::: List(actionFactory.startUpdate(this), actionFactory.startDelete(this))

  protected def getReadOnlyEntityActions(actionFactory: UIActionFactory): List[UIAction[ID]] =
    displayLayout.map(_ => actionFactory.display(this)).toList :::
            displayChildEntityLists[ID](actionFactory, id => id, childEntities(actionFactory.application)) ::: Nil

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
      verbose("Added value at position " + position + " to the cache for " + activity)
    }

    def cacheClearingObserver(activity: ListActivity) = new DataSetObserver {
      override def onInvalidated() {
        verbose("Clearing ListView cache in " + activity + " since DataSet was invalidated")
        activity.runOnUiThread { activity.getListView.setTag(null) }
        super.onInvalidated()
      }
    }

    protected def bindViewFromCacheOrItems(view: View, itemsToCopyAtPosition: => List[AnyRef], position: Long, activity: ListActivity) {
      val cachedValue: Option[PortableValue] = findCachedPortableValue(activity, position)
      //set the cached or default values immediately instead of showing the column header names
      cachedValue match {
        case Some(portableValue) =>
          verbose("cache hit for " + activity + " at position " + position + ": " + portableValue)
          portableValue.copyTo(view)
        case None =>
          verbose("cache miss for " + activity + " at position " + position)
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
            val parentView = view.getParent.asInstanceOf[AdapterView[_]]
            parentView.invalidate()
          }
        }
      }
    }
  }

  final def setListAdapter(crudContext: CrudContext, activity: ListActivity) {
    val persistence = openEntityPersistence(crudContext)
    persistenceVarForListAdapter.set(crudContext, persistence)
    setListAdapter(persistence, crudContext, activity)
  }

  def setListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: ListActivity)

  def refreshAfterSave(crudContext: CrudContext)

  def destroyContextVars(crudContext: CrudContext) {
    persistenceVarForListAdapter.clear(crudContext).map(_.close())
  }

  private[crud] def undoableDelete(id: ID, uiActionFactory: UIActionFactory)(persistence: EntityPersistence) {
    persistence.find(id).map { readable =>
      val writable = newWritable
      copy(readable, writable)
      persistence.delete(List(id))
      uiActionFactory.addUndoableDelete(this, new Undoable[ID] {
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
  def startDelete(id: ID, uiActionFactory: UIActionFactory) {
    uiActionFactory.withEntityPersistence(this, undoableDelete(id, uiActionFactory))
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
