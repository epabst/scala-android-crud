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
  def displayLayout: Option[Int]
  def entryLayout: Int

  final def hasDisplayPage = displayLayout.isDefined

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
        val getForeignKey = { _: Unit => foreignKey.partialGet(actionFactory.currentIntent).get }
        actionFactory.adapt(actionFactory.startUpdate(parentEntity), getForeignKey) ::
                parentEntity.displayChildEntityLists(actionFactory, getForeignKey,
                  parentEntity.childEntities(actionFactory.application).filter(_ != thisEntity))
      }
      case _ => Nil
    }) ::: actionFactory.startCreate(this) :: Nil
  }

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
  def getEntityActions(actionFactory: UIActionFactory): List[UIAction[ID]] =
    displayLayout.map(_ => actionFactory.display(this)).toList :::
            displayChildEntityLists[ID](actionFactory, id => id, childEntities(actionFactory.application)) :::
            List(actionFactory.startUpdate(this), actionFactory.startDelete(this))

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

trait CrudEntityTypeRef extends PlatformTypes {
  //this is the type used for internationalized strings
  def entityName: String

  def hasDisplayPage: Boolean

  def listItemsString: Option[Int] = None
  //todo replace these with using the standard android icons
  def addItemString: Int
  def editItemString: Int
  def deleteItemString: Int = res.R.string.delete_item
  def cancelItemString: Int

  def parentEntities: List[CrudEntityTypeRef]

  /**
   * The list of entities that refer to this one.
   * Those entities should have a foreignKey in their fields list, if persisted.
   */
  def childEntities(application: CrudApplication): List[CrudEntityTypeRef] =
    application.allEntities.filter(_.parentEntities.contains(this))

  def displayChildEntityLists[T](actionFactory: UIActionFactory, idGetter: T => ID,
                                 childEntities: List[CrudEntityTypeRef]): List[UIAction[T]] =
    childEntities.map(entity => actionFactory.adapt(actionFactory.displayList(entity), (value: T) =>
      Some(EntityUriSegment(entityName, idGetter(value).toString))))

  def listActivityClass: Class[_ <: CrudListActivity[_,_,_,_]]
  def activityClass: Class[_ <: CrudActivity[_,_,_,_]]

  override def toString = entityName
}
