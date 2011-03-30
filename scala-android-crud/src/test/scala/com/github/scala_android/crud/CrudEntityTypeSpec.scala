package com.github.scala_android.crud

import org.junit.runner.RunWith
import scala.collection.mutable
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import com.github.scala_android.crud.CursorFieldAccess._
import org.easymock.EasyMock.notNull
import com.github.triangle.Field

/**
 * A behavior specification for {@link CrudEntityType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/22/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class CrudEntityTypeSpec extends Spec with ShouldMatchers with MyEntityTesting {

  it("should derive parent entities from foreignKey fields") {
    val persistence = mock[EntityPersistence[AnyRef,List[mutable.Map[String,Any]],mutable.Map[String,Any],mutable.Map[String,Any]]]
    val entityType1 = new MyEntityType(persistence)
    val entityType2 = new MyEntityType(persistence)
    val entityType3 = new MyEntityType(persistence) {
      override val fields = Field(foreignKey(entityType1)) :: Field(foreignKey(entityType2)) :: super.fields
    }
    entityType3.parentEntities should be (List(entityType1, entityType2))
  }

  val startCreateParent = namedMock[UIAction[Unit]]("startCreateParent")
  val startUpdateParent = namedMock[UIAction[Long]]("startUpdateParent")
  val adaptedStartUpdateParent = namedMock[UIAction[Unit]]("adaptedStartUpdateParent")
  val startDeleteParent = namedMock[UIAction[ID]]("startDeleteParent")
  val displayChildList = namedMock[UIAction[EntityUriSegment]]("displayChildList")
  val adaptedDisplayChildList = namedMock[UIAction[ID]]("adaptedDisplayChildList")
  val startCreateChild = namedMock[UIAction[Unit]]("startCreateChild")
  val startUpdateChild = namedMock[UIAction[Unit]]("startUpdateChild")
  val startDeleteChild = namedMock[UIAction[ID]]("startDeleteChild")
  val displayChild2List = namedMock[UIAction[EntityUriSegment]]("displayChild2List")
  val adaptedDisplayChild2List = namedMock[UIAction[ID]]("adaptedDisplayChild2List")

  it("should get the correct entity actions with child entities") {
    val persistence = mock[EntityPersistence[AnyRef,List[mutable.Map[String,Any]],mutable.Map[String,Any],mutable.Map[String,Any]]]
    val actionFactory = mock[UIActionFactory]
    var parentEntityOption: Option[MyEntityType] = None
    val childEntity = new MyEntityType(persistence) {
      override lazy val fields = Field(foreignKey(parentEntityOption.get)) :: super.fields
    }
    parentEntityOption = Some(new MyEntityType(persistence) {
      override def childEntities = childEntity :: super.childEntities
    })
    val parentEntity = parentEntityOption.get
    expecting {
      call(actionFactory.displayList(childEntity)).andReturn(displayChildList)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChildList), notNull())).andReturn(adaptedDisplayChildList)
      call(actionFactory.startUpdate(parentEntity)).andReturn(startUpdateParent)
      call(actionFactory.startDelete(parentEntity)).andReturn(startDeleteParent)
      call(actionFactory.startUpdate(childEntity)).andReturn(startUpdateChild)
      call(actionFactory.startDelete(childEntity)).andReturn(startDeleteChild)
    }
    whenExecuting(actionFactory) {
      childEntity.getEntityActions(actionFactory) should be (List(startUpdateChild, startDeleteChild))
      parentEntity.getEntityActions(actionFactory) should be (List(adaptedDisplayChildList, startUpdateParent, startDeleteParent))
    }
  }

  it("should get the correct list actions with child entities") {
    val persistence = mock[EntityPersistence[AnyRef,List[mutable.Map[String,Any]],mutable.Map[String,Any],mutable.Map[String,Any]]]
    val actionFactory = mock[UIActionFactory]
    var parentEntityOption: Option[MyEntityType] = None
    val childEntity = new MyEntityType(persistence) {
      override lazy val fields = Field(foreignKey(parentEntityOption.get)) :: super.fields
    }
    val childEntity2 = new MyEntityType(persistence) {
      override lazy val fields = Field(foreignKey(parentEntityOption.get)) :: super.fields
    }
    parentEntityOption = Some(new MyEntityType(persistence) {
      override val displayLayout = Some(123)

      override def childEntities = childEntity :: childEntity2 :: super.childEntities
    })
    val parentEntity = parentEntityOption.get
    expecting {
      call(actionFactory.startCreate(parentEntity)).andReturn(startCreateParent)
      call(actionFactory.startCreate(childEntity)).andReturn(startCreateChild)
      call(actionFactory.displayList(childEntity)).andStubReturn(displayChildList)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChildList), notNull())).andStubReturn(adaptedDisplayChildList)
      call(actionFactory.displayList(childEntity2)).andStubReturn(displayChild2List)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChild2List), notNull())).andStubReturn(adaptedDisplayChild2List)
    }
    whenExecuting(actionFactory) {
      parentEntity.getListActions(actionFactory) should be (List(startCreateParent))
      childEntity.getListActions(actionFactory) should be (List(startCreateChild))
    }
  }

  it("should get the correct list actions with child entities w/ no parent display") {
    val persistence = mock[EntityPersistence[AnyRef,List[mutable.Map[String,Any]],mutable.Map[String,Any],mutable.Map[String,Any]]]
    val actionFactory = mock[UIActionFactory]
    var parentEntityOption: Option[MyEntityType] = None
    val childEntity = new MyEntityType(persistence) {
      override lazy val fields = Field(foreignKey(parentEntityOption.get)) :: super.fields
    }
    val childEntity2 = new MyEntityType(persistence) {
      override lazy val fields = Field(foreignKey(parentEntityOption.get)) :: super.fields
    }
    parentEntityOption = Some(new MyEntityType(persistence) {
      override def childEntities = childEntity :: childEntity2 :: super.childEntities
    })
    val parentEntity = parentEntityOption.get
    expecting {
      call(actionFactory.startCreate(parentEntity)).andReturn(startCreateParent)
      call(actionFactory.startUpdate(parentEntity)).andReturn(startUpdateParent)
      call(actionFactory.adapt[Unit,Long](eql(startUpdateParent), notNull())).andReturn(adaptedStartUpdateParent)
      call(actionFactory.startCreate(childEntity)).andReturn(startCreateChild)
      call(actionFactory.displayList(childEntity)).andStubReturn(displayChildList)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChildList), notNull())).andStubReturn(adaptedDisplayChildList)
      call(actionFactory.displayList(childEntity2)).andReturn(displayChild2List)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChild2List), notNull())).andReturn(adaptedDisplayChild2List)
    }
    whenExecuting(actionFactory) {
      parentEntity.getListActions(actionFactory) should be (List(startCreateParent))
      childEntity.getListActions(actionFactory) should be (List(adaptedStartUpdateParent, adaptedDisplayChild2List, startCreateChild))
    }
  }
}