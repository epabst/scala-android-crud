package com.github.scala_android.crud

import android.widget.ListAdapter
import android.app.Activity
import android.net.Uri
import com.github.triangle.{FieldList, BaseField}

/**
 * An entity configuration that provides all custom information needed to
 * implement CRUD on the entity.  This shouldn't depend on the platform (e.g. android).
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:24 PM
 */
trait CrudType extends CrudTypeRef {
  def headerLayout: LayoutKey
  def listLayout: LayoutKey
  def rowLayout: LayoutKey
  def displayLayout: Option[LayoutKey]
  def entryLayout: LayoutKey

  final def hasDisplayPage = displayLayout.isDefined

  private val persistenceVarForListAdapter = new ContextVar[CrudPersistence]

  /**
   * Gets the actions that a user can perform from a list of the entities.
   * May be overridden to modify the list of actions.
   */
  def getListActions(actionFactory: UIActionFactory): List[UIAction[Unit]] = {
    val thisEntity = this;
    (foreignKeys match {
      //exactly one parent w/o a display page
      case foreignKey :: Nil if !foreignKey.entityType.hasDisplayPage => {
        val parentEntity = foreignKey.entityType
        val getForeignKey = { _: Unit => foreignKey(actionFactory.currentIntent) }
        actionFactory.adapt(actionFactory.startUpdate(parentEntity), getForeignKey) ::
                parentEntity.displayChildEntityLists(actionFactory, getForeignKey,
                  parentEntity.childEntities(actionFactory.application).filter(_ != thisEntity))
      }
      case _ => Nil
    }) ::: actionFactory.startCreate(this) :: Nil
  }

  /**
   * Gets the actions that a user can perform from a specific entity instance.
   * The first one is the one that will be used when the item is clicked on.
   * May be overridden to modify the list of actions.
   */
  def getEntityActions(actionFactory: UIActionFactory): List[UIAction[ID]] =
    displayLayout.map(_ => actionFactory.display(this)).toList :::
            displayChildEntityLists[ID](actionFactory, id => id, childEntities(actionFactory.application)) :::
            List(actionFactory.startUpdate(this), actionFactory.startDelete(this))

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

  final def createListAdapter(crudContext: CrudContext, activity: Activity): ListAdapter = {
    val persistence = openEntityPersistence(crudContext)
    persistenceVarForListAdapter.set(crudContext, persistence)
    createListAdapter(persistence, crudContext, activity)
  }

  def createListAdapter(persistence: CrudPersistence, crudContext: CrudContext, activity: Activity): ListAdapter

  def refreshAfterSave(crudContext: CrudContext)

  def destroyContextVars(crudContext: CrudContext) {
    persistenceVarForListAdapter.clear(crudContext).map(_.close())
  }

  private[crud] def undoableDelete(id: ID, uiActionFactory: UIActionFactory)(persistence: EntityPersistence) {
    persistence.find(id).map { readable =>
      val writable = newWritable
      copyFields(readable, writable)
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

trait CrudTypeRef extends FieldList with PlatformTypes {
  //this is the type used for internationalized strings
  def entityName: String

  def fields: List[BaseField]

  def hasDisplayPage: Boolean

  def listItemsString: Option[SKey] = None
  def addItemString: SKey
  def editItemString: SKey
  def deleteItemString: SKey = res.R.string.delete_item
  def cancelItemString: SKey

  lazy val foreignKeys: List[ForeignKey] = fieldFlatMap {
    case foreignKey: ForeignKey => Some(foreignKey)
  }

  lazy val parentEntities: List[CrudTypeRef] = foreignKeys.map(_.entityType)

  /**
   * The list of entities that refer to this one.
   * Those entities should have a foreignKey in their fields list, if persisted.
   */
  def childEntities(application: CrudApplication): List[CrudTypeRef] =
    application.allEntities.filter(_.parentEntities.contains(this))

  def displayChildEntityLists[T](actionFactory: UIActionFactory, idGetter: T => ID,
                                 childEntities: List[CrudTypeRef]): List[UIAction[T]] =
    childEntities.map(entity => actionFactory.adapt(actionFactory.displayList(entity), (value: T) =>
      Some(EntityUriSegment(entityName, idGetter(value).toString))))

  def listActivityClass: Class[_ <: CrudListActivity]
  def activityClass: Class[_ <: CrudActivity]

  def findId(uri: Uri): Option[ID] = new EntityUriSegment(entityName).findId(uri)

  override def toString = entityName
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