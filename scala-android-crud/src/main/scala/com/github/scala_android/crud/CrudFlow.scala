package com.github.scala_android.crud

import android.app.Activity

/**
 * A point (representing an Activity) in the CrudFlow.
 */
abstract class CrudFlowPoint(val crudType: CrudEntityTypeRef) {
  /**
   * Gets the available actions.
   */
  def getActions(currentActivity: Activity): List[UIAction]

  /**
   * Gets the available actions when choosing an item from a list.
   */
  def getItemActions(itemId: Long, currentActivity: Activity): List[UIAction]
}

/**
 * Something that uses {@link ActivityRef}s.
 */
trait ActivityRefConsumer[R,LR] {
  /**
   * The activity for creating an entity.
   */
  def create(crudType: CrudEntityType[_,_,_,_]): R

  /**
   * The activity for listing entities.
   */
  def listOf(crudType: CrudEntityType[_,_,_,_]): LR

  /**
   * The activity for displaying an entity.
   */
  def display(crudType: CrudEntityType[_,_,_,_]): R

  /**
   * The activity of updating an entity.
   */
  def update(crudType: CrudEntityType[_,_,_,_]): R

  /**
   * The activity of deleting an entity (with confirmation).
   */
  def delete(crudType: CrudEntityType[_,_,_,_]): R
}

/**
 * A reference to an Activity.
 */
trait ActivityRef extends TransitionBuilder

/**
 * A reference to a ListActivity.
 */
trait ListActivityRef extends ActivityRef with ListTransitionBuilder

/**
 * A builder for specifying the available transitions from one Activity to another.
 */
trait TransitionBuilder {
  /**
   * Sets the list of options.  These options are presented to the user upon pressing the menu button.
   * @returns self for chaining.
   */
  def withOptions(targets: ActivityRef*): TransitionBuilder

  /**
   * Adds to the list of options.
   * These options are presented to the user upon pressing the menu button in addition to the default options.
   * @returns self for chaining.
   */
  def withAddedOptions(targets: ActivityRef*): TransitionBuilder

  /**
   * Gets the options, including the defaults.
   */
  def options: List[ActivityRef]
}

trait ListTransitionBuilder extends TransitionBuilder {
  /**
   * Sets the list of item options.
   * The first option is the one used when the user clicks on an item in the list.
   * The rest are presented when long-clicking on an item in the list.
   * @returns self for chaining.
   */
  def withItemOptions(targets: ActivityRef*): ListTransitionBuilder

  /**
   * Adds to the list of item options.
   * @returns self for chaining.
   */
  def withAddedItemOptions(targets: ActivityRef*): ListTransitionBuilder

  /**
   * Gets the item options, including the defaults.
   */
  def itemOptions: List[ActivityRef]
}

/**
 * The flow between Crud Activities and other Activities.
 * <p>
 * An application should have a CrudFlow object that has:
 * <pre>
 *   startWith listOf(BarEntity)
 *
 *   //a Bar has a list of Foo's
 *   display(BarEntity) withAddedOptions(listOf(FooEntity))
 *
 *   //these are included by default, so these lines are all optional
 *   listOf(FooEntity) withItemOptions(display(FooEntity), update(FooEntity), delete(FooEntity)) withOptions(create(FooEntity))
 *   display(FooEntity) withOptions(update(FooEntity), delete(FooEntity))
 *   update(FooEntity) withOptions(delete(FooEntity))
 * </pre>
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 10:01 PM
 */

trait CrudFlow extends ActivityRefConsumer[ActivityRef,ListActivityRef] {
  private var _entityTypes: Set[CrudEntityType[_,_,_,_]] = Set[CrudEntityType[_,_,_,_]]()
  /**
   * Indicates which activity the application should start with.
   */
  protected def startWith: ActivityRefConsumer[Unit,Unit] = {
    new ActivityRefConsumer[Unit,Unit] {
      private def addEntityType(crudType: CrudEntityType[_, _, _, _]) {
        _entityTypes += crudType
      }

      def listOf(crudType: CrudEntityType[_, _, _, _]) = addEntityType(crudType)

      def delete(crudType: CrudEntityType[_, _, _, _]) = addEntityType(crudType)

      def update(crudType: CrudEntityType[_, _, _, _]) = addEntityType(crudType)

      def display(crudType: CrudEntityType[_, _, _, _]) = addEntityType(crudType)

      def create(crudType: CrudEntityType[_, _, _, _]) = addEntityType(crudType)
    }
  }

  class FlowActivityRef(crudType: CrudEntityType[_, _, _, _]) extends ActivityRef {
    _entityTypes += crudType

    def options = List[ActivityRef]()

    def withAddedOptions(targets: ActivityRef*) = this

    def withOptions(targets: ActivityRef*) = this
  }

  class FlowListActivityRef(crudType: CrudEntityType[_, _, _, _]) extends FlowActivityRef(crudType) with ListActivityRef {
    def itemOptions = List[ActivityRef]()

    def withAddedItemOptions(targets: ActivityRef*) = this

    def withItemOptions(targets: ActivityRef*) = this
  }

  def delete(crudType: CrudEntityType[_, _, _, _]) = new FlowActivityRef(crudType)

  def update(crudType: CrudEntityType[_, _, _, _]) = new FlowActivityRef(crudType)

  def display(crudType: CrudEntityType[_, _, _, _]) = new FlowActivityRef(crudType)

  def listOf(crudType: CrudEntityType[_, _, _, _]) = new FlowListActivityRef(crudType)

  def create(crudType: CrudEntityType[_, _, _, _]) = new FlowActivityRef(crudType)

//  val startingPoint: CrudFlowPoint

  //once invoked, no more changes may be done
  lazy val entityTypes: Set[CrudEntityType[_,_,_,_]] = _entityTypes
}
