package com.github.scala_android.crud

/**
 * The flow between Crud Activities and other Activities.
 * <p>
 * An application should have a CrudFlow object that has:
 * <pre>
 *   startWith listOf(BarEntity)
 *
 *   //a Bar has a list of Foo's
 *   display(BarEntity) hasAddedOptions(listOf(FooEntity))
 *
 *   //these are included by default, so these lines are all optional
 *   listOf(FooEntity) hasItemOptions(display(FooEntity), update(FooEntity), delete(FooEntity)) hasOptions(create(FooEntity))
 *   display(FooEntity) hasOptions(update(FooEntity), delete(FooEntity))
 *   update(FooEntity) hasOptions(delete(FooEntity))
 * </pre>
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/11/11
 * Time: 10:01 PM
 */

trait CrudFlow extends ActivityRefConsumer[ActivityRef,ListActivityRef] {
  /**
   * Indicates which activity the application should start with.
   */
  def startWith: ActivityRefConsumer[Unit,Unit]
}

/**
 * Something that uses {@link ActivityRef}s.
 */
trait ActivityRefConsumer[R,LR] {
  /**
   * The activity for creating an entity.
   */
  def create(crudConfig: CrudEntityConfig[_,_,_,_]): R

  /**
   * The activity for listing entities.
   */
  def listOf(crudConfig: CrudEntityConfig[_,_,_,_]): LR

  /**
   * The activity for displaying an entity.
   */
  def display(crudConfig: CrudEntityConfig[_,_,_,_]): R

  /**
   * The activity of updating an entity.
   */
  def update(crudConfig: CrudEntityConfig[_,_,_,_]): R

  /**
   * The activity of deleting an entity (with confirmation).
   */
  def delete(crudConfig: CrudEntityConfig[_,_,_,_]): R
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
  def hasOptions(targets: ActivityRef*): TransitionBuilder

  /**
   * Adds to the list of options.
   * These options are presented to the user upon pressing the menu button in addition to the default options.
   * @returns self for chaining.
   */
  def hasAddedOptions(targets: ActivityRef*): TransitionBuilder

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
  def hasItemOptions(targets: ActivityRef*): ListTransitionBuilder

  /**
   * Adds to the list of item options.
   * @returns self for chaining.
   */
  def hasAddedItemOptions(targets: ActivityRef*): ListTransitionBuilder

  /**
   * Gets the item options, including the defaults.
   */
  def itemOptions: List[ActivityRef]
}