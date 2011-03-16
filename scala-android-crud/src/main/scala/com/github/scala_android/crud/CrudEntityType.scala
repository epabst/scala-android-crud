package com.github.scala_android.crud

import android.content.Context
import com.github.triangle.CopyableField

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
  //this makes it available for subtypes to use to make it clear that it's an ID
  type ID = Long

  def fields: List[CopyableField]

  def headerLayout: Int
  def listLayout: Int
  def rowLayout: Int
  def entryLayout: Int

  /**
   * Gets the actions that a user can perform from a list of the entities.
   * May be overridden to modify the list of actions.
   */
  def getListActions(actionFactory: UIActionFactory): List[UIAction] =
    List(actionFactory.displayList(this), actionFactory.startCreate(this))

  /**
   * Gets the actions that a user can perform from a specific entity instance.
   * The first one is the one that will be used when the item is clicked on.
   * May be overridden to modify the list of actions.
   */
  def getEntityActions(actionFactory: UIActionFactory, id: ID): List[UIAction] =
    List(actionFactory.display(this, id), actionFactory.startUpdate(this, id), actionFactory.startDelete(this, List(id)))

  def copyFields(from: AnyRef, to: AnyRef) {
    fields.foreach(_.copy(from, to))
  }

  def openEntityPersistence(context: Context): EntityPersistence[Q,L,R,W]
}

trait CrudEntityTypeRef {
  //this is the type used for internationalized strings
  def entityName: String

  def listItemsString: Option[Int] = None
  //todo replace these with using the standard android icons
  def addItemString: Int
  def editItemString: Int
  def cancelItemString: Int

  def listActivityClass: Class[_ <: CrudListActivity[_,_,_,_]]
  def activityClass: Class[_ <: CrudActivity[_,_,_,_]]

  override def toString = entityName
}
