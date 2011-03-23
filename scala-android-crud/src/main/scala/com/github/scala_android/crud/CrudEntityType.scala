package com.github.scala_android.crud

import android.content.Context
import android.widget.ListAdapter
import com.github.triangle.{PartialFieldAccess, CopyableField}

/**
 * An entity configuration that provides all custom information needed to
 * implement CRUD on the entity.  This shouldn't depend on the platform (e.g. android).
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/23/11
 * Time: 3:24 PM
 * @param Int a translatable text identifier
 * @param LT a layout configuration
 */
trait CrudEntityType[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef] extends CrudEntityTypeRef {
  def fields: List[CopyableField]

  def headerLayout: Int
  def listLayout: Int
  def rowLayout: Int
  def displayLayout: Option[Int] = None
  def entryLayout: Int

  final def hasDisplayPage = displayLayout.isDefined

  /**
   * Gets the actions that a user can perform from a list of the entities.
   * May be overridden to modify the list of actions.
   */
  def getListActions(actionFactory: UIActionFactory): List[UIAction] =
    (foreignKeys match {
      //exactly one parent w/o a display page
      case foreignKey :: Nil if !foreignKey.entityType.hasDisplayPage => {
        val parentEntity = foreignKey.entityType
        val foreignId = foreignKey.partialGet(actionFactory.currentIntent).get
        actionFactory.startUpdate(parentEntity, foreignId) :: parentEntity.displayChildEntityLists(actionFactory, foreignId)
      }
      case _ => Nil
    }) ::: actionFactory.startCreate(this) :: Nil

  lazy val foreignKeys: List[ForeignKey] = fieldAccessFlatMap(_ match {
    case foreignKey: ForeignKey => Some(foreignKey)
    case _ => None
  })

  lazy val parentEntities: List[CrudEntityTypeRef] = foreignKeys.map(_.entityType)

  def fieldAccessFlatMap[B](f: (PartialFieldAccess[_]) => Traversable[B]): List[B] =
    CursorFieldAccess.fieldAccessFlatMap(fields, f)

  /**
   * Gets the actions that a user can perform from a specific entity instance.
   * The first one is the one that will be used when the item is clicked on.
   * May be overridden to modify the list of actions.
   */
  def getEntityActions(actionFactory: UIActionFactory, id: ID): List[UIAction] =
    displayLayout.map[UIAction](_ => actionFactory.display(this, id)).toList :::
            displayChildEntityLists(actionFactory, id) :::
            List(actionFactory.startUpdate(this, id), actionFactory.startDelete(this, List(id)))

  def copyFields(from: AnyRef, to: AnyRef) {
    fields.foreach(_.copy(from, to))
  }

  def openEntityPersistence(context: Context): EntityPersistence[Q,L,R,W]

  def withEntityPersistence[T](context: Context, f: EntityPersistence[Q,L,R,W] => T): T = {
    val persistence = openEntityPersistence(context)
    try f(persistence)
    finally persistence.close
  }

  def refreshAfterSave(listAdapter: ListAdapter)
}

trait CrudEntityTypeRef {
  //this makes it available for subtypes to use to make it clear that it's an ID
  type ID = Long

  //this is the type used for internationalized strings
  def entityName: String

  def hasDisplayPage: Boolean

  def listItemsString: Option[Int] = None
  //todo replace these with using the standard android icons
  def addItemString: Int
  def editItemString: Int
  def cancelItemString: Int

  /**
   * The list of entities that refer to this one.
   * Those entities should have a foreignKey in their fields list, if persisted.
   */
  def childEntities: List[CrudEntityTypeRef]

  def displayChildEntityLists(actionFactory: UIActionFactory, id: ID): List[UIAction] =
    childEntities.map(entity => actionFactory.displayList(entity, Some(EntityUriSegment(entityName, id.toString))))

  def listActivityClass: Class[_ <: CrudListActivity[_,_,_,_]]
  def activityClass: Class[_ <: CrudActivity[_,_,_,_]]

  override def toString = entityName
}
