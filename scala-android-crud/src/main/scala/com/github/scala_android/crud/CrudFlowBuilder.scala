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
 * Something that uses {@link CrudFlowPoint}s.
 */
trait CrudFlowPointConsumer[R,LR] {
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
trait CrudFlowPointBuilder extends TransitionBuilder

/**
 * A reference to a ListActivity.
 */
trait ListCrudFlowPointBuilder extends CrudFlowPointBuilder with ListTransitionBuilder

/**
 * A builder for specifying the available transitions from one Activity to another.
 */
trait TransitionBuilder {
  /**
   * Sets the list of options.  These options are presented to the user upon pressing the menu button.
   * @returns self for chaining.
   */
  def withOptions(targets: CrudFlowPointBuilder*): TransitionBuilder

  /**
   * Adds to the list of options.
   * These options are presented to the user upon pressing the menu button in addition to the default options.
   * @returns self for chaining.
   */
  def withAddedOptions(targets: CrudFlowPointBuilder*): TransitionBuilder

  /**
   * Gets the options, including the defaults.
   */
  def options: List[CrudFlowPointBuilder]
}

trait ListTransitionBuilder extends TransitionBuilder {
  /**
   * Sets the list of item options.
   * The first option is the one used when the user clicks on an item in the list.
   * The rest are presented when long-clicking on an item in the list.
   * @returns self for chaining.
   */
  def withItemOptions(targets: CrudFlowPointBuilder*): ListTransitionBuilder

  /**
   * Adds to the list of item options.
   * @returns self for chaining.
   */
  def withAddedItemOptions(targets: CrudFlowPointBuilder*): ListTransitionBuilder

  /**
   * Gets the item options, including the defaults.
   */
  def itemOptions: List[CrudFlowPointBuilder]
}

/**
 * The flow between Crud Activities and other Activities.
 * <p>
 * An application should have a CrudFlowBuilder object that has:
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

trait CrudFlowBuilder extends CrudFlowPointConsumer[CrudFlowPointBuilder,ListCrudFlowPointBuilder] {
  private var _entityTypes: Set[CrudEntityType[_,_,_,_]] = Set[CrudEntityType[_,_,_,_]]()
  /**
   * Indicates which activity the application should start with.
   */
  protected def startWith: CrudFlowPointConsumer[Unit,Unit] = {
    new CrudFlowPointConsumer[Unit,Unit] {
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

  private[crud] class PointBuilder(crudType: CrudEntityType[_, _, _, _]) extends CrudFlowPointBuilder {
    _entityTypes += crudType

    def options = List[CrudFlowPointBuilder]()

    def withAddedOptions(targets: CrudFlowPointBuilder*) = this

    def withOptions(targets: CrudFlowPointBuilder*) = this
  }

  private[crud] class ListPointBuilder(crudType: CrudEntityType[_, _, _, _]) extends PointBuilder(crudType) with ListCrudFlowPointBuilder {
    def itemOptions = List[CrudFlowPointBuilder]()

    def withAddedItemOptions(targets: CrudFlowPointBuilder*): ListCrudFlowPointBuilder = this

    def withItemOptions(targets: CrudFlowPointBuilder*): ListCrudFlowPointBuilder = this
  }

  def delete(crudType: CrudEntityType[_, _, _, _]): CrudFlowPointBuilder = new PointBuilder(crudType)

  def update(crudType: CrudEntityType[_, _, _, _]): CrudFlowPointBuilder = new PointBuilder(crudType)

  def display(crudType: CrudEntityType[_, _, _, _]): CrudFlowPointBuilder = new PointBuilder(crudType)

  def listOf(crudType: CrudEntityType[_, _, _, _]): ListCrudFlowPointBuilder = new ListPointBuilder(crudType)

  def create(crudType: CrudEntityType[_, _, _, _]): CrudFlowPointBuilder = new PointBuilder(crudType)

//  val startingPoint: CrudFlowPoint

  //once invoked, no more changes may be done
  lazy val entityTypes: Set[CrudEntityType[_,_,_,_]] = _entityTypes
}
