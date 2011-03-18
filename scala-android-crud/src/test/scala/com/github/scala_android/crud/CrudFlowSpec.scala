package com.github.scala_android.crud

import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import scala.collection.mutable

/**
 * A test for {@link CrudFlow}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/18/11
 * Time: 6:22 PM
 */
@RunWith(classOf[JUnitRunner])
class CrudFlowSpec extends Spec with ShouldMatchers with MyEntityTesting {
  it("should identify the CrudEntityType from startWith") {
    shouldIdentifyTheCrudEntityTypeFromStartWith(entityType => _.listOf(entityType))
    shouldIdentifyTheCrudEntityTypeFromStartWith(entityType => _.create(entityType))
    shouldIdentifyTheCrudEntityTypeFromStartWith(entityType => _.display(entityType))
    shouldIdentifyTheCrudEntityTypeFromStartWith(entityType => _.update(entityType))
    shouldIdentifyTheCrudEntityTypeFromStartWith(entityType => _.delete(entityType))
  }

  it("should identify the CrudEntityTypes from direct ActivityRefs") {
    val entityPersistence = mock[EntityPersistence[AnyRef,List[mutable.Map[String,Any]],mutable.Map[String,Any],mutable.Map[String,Any]]]
    val myEntityType1 = new MyEntityType(entityPersistence)
    val myEntityType2 = new MyEntityType(entityPersistence)
    val myEntityType3 = new MyEntityType(entityPersistence)
    val myEntityType4 = new MyEntityType(entityPersistence)
    val myEntityType5 = new MyEntityType(entityPersistence)
    object MyCrudFlow extends CrudFlow {
      listOf(myEntityType1)
      create(myEntityType2)
      display(myEntityType3)
      update(myEntityType4)
      delete(myEntityType5)
    }
    whenExecuting(entityPersistence) {
      MyCrudFlow.entityTypes should be (Set(myEntityType1, myEntityType2, myEntityType3, myEntityType4, myEntityType5))
    }
  }

  it("should identify the CrudEntityTypes from options") {
    val entityPersistence = mock[EntityPersistence[AnyRef,List[mutable.Map[String,Any]],mutable.Map[String,Any],mutable.Map[String,Any]]]
    val myEntityType1 = new MyEntityType(entityPersistence)
    val myEntityType2 = new MyEntityType(entityPersistence)
    val myEntityType3 = new MyEntityType(entityPersistence)
    val myEntityType4 = new MyEntityType(entityPersistence)
    val myEntityType5 = new MyEntityType(entityPersistence)
    val myEntityType6 = new MyEntityType(entityPersistence)
    object MyCrudFlow extends CrudFlow {
      listOf(myEntityType1).
              hasItemOptions(create(myEntityType2), display(myEntityType3)).
              hasOptions(update(myEntityType4), delete(myEntityType5), listOf(myEntityType6))
    }
    whenExecuting(entityPersistence) {
      MyCrudFlow.entityTypes should be (Set(myEntityType1, myEntityType2, myEntityType3, myEntityType4, myEntityType5, myEntityType6))
    }
  }

  def shouldIdentifyTheCrudEntityTypeFromStartWith(f: CrudEntityType[_,_,_,_] => ActivityRefConsumer[Unit,Unit] => Unit) {
    val entityPersistence = mock[EntityPersistence[AnyRef,List[mutable.Map[String,Any]],mutable.Map[String,Any],mutable.Map[String,Any]]]
    val myEntityType = new MyEntityType(entityPersistence)
    object MyCrudFlow extends CrudFlow {
      f(myEntityType)(startWith)
    }
    whenExecuting(entityPersistence) {
      MyCrudFlow.entityTypes should be (Set(myEntityType))
    }
  }
}