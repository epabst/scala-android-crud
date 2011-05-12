package com.github.scala_android.crud

import org.junit.runner.RunWith
import scala.collection.mutable
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.Spec
import com.github.scala_android.crud.CursorField._
import org.easymock.EasyMock
import EasyMock.notNull
import com.github.triangle.PortableField
import android.widget.ListAdapter

/**
 * A behavior specification for {@link CrudType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/22/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class CrudTypeSpec extends Spec with ShouldMatchers with MyEntityTesting {

  it("should derive parent entities from foreignKey fields") {
    val persistence = mock[CrudPersistence]
    val listAdapter = mock[ListAdapter]
    whenExecuting(persistence, listAdapter) {
      val entityType1 = new MyEntityType(persistence, listAdapter)
      val entityType2 = new MyEntityType(persistence, listAdapter)
      val entityType3 = new MyEntityType(persistence, listAdapter) {
        override val fields = foreignKey(entityType1) :: foreignKey(entityType2) :: super.fields
      }
      entityType3.parentEntities should be (List(entityType1, entityType2))
    }
  }

  val startCreateParent = namedMock[UIAction[Unit]]("startCreateParent")
  val startUpdateParent = namedMock[UIAction[ID]]("startUpdateParent")
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
    val persistence = mock[CrudPersistence]
    val actionFactory = mock[UIActionFactory]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val parentEntity = new MyEntityType(persistence, listAdapter)
    val childEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val fields = foreignKey(parentEntity) :: super.fields
    }
    expecting {
      call(actionFactory.application).andReturn(application).anyTimes
      call(application.allEntities).andReturn(List(parentEntity, childEntity)).anyTimes
      call(actionFactory.displayList(childEntity)).andReturn(displayChildList)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChildList), notNull())).andReturn(adaptedDisplayChildList)
      call(actionFactory.startUpdate(parentEntity)).andReturn(startUpdateParent)
      call(actionFactory.startDelete(parentEntity)).andReturn(startDeleteParent)
      call(actionFactory.startUpdate(childEntity)).andReturn(startUpdateChild)
      call(actionFactory.startDelete(childEntity)).andReturn(startDeleteChild)
    }
    whenExecuting(actionFactory, persistence, application) {
      childEntity.getEntityActions(actionFactory) should be (List(startUpdateChild, startDeleteChild))
      parentEntity.getEntityActions(actionFactory) should be (List(adaptedDisplayChildList, startUpdateParent, startDeleteParent))
    }
  }

  it("should get the correct list actions with child entities") {
    val persistence = mock[CrudPersistence]
    val actionFactory = mock[UIActionFactory]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val parentEntity = new MyEntityType(persistence, listAdapter) {
      override val displayLayout = Some(123)
    }
    val childEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val fields = foreignKey(parentEntity) :: super.fields
    }
    val childEntity2 = new MyEntityType(persistence, listAdapter) {
      override lazy val fields = foreignKey(parentEntity) :: super.fields
    }
    expecting {
      call(actionFactory.application).andReturn(application).anyTimes
      call(application.allEntities).andReturn(List(parentEntity, childEntity, childEntity2)).anyTimes
      call(actionFactory.startCreate(parentEntity)).andReturn(startCreateParent)
      call(actionFactory.startCreate(childEntity)).andReturn(startCreateChild)
      call(actionFactory.displayList(childEntity)).andStubReturn(displayChildList)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChildList), notNull())).andStubReturn(adaptedDisplayChildList)
      call(actionFactory.displayList(childEntity2)).andStubReturn(displayChild2List)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChild2List), notNull())).andStubReturn(adaptedDisplayChild2List)
    }
    whenExecuting(actionFactory, persistence, application) {
      parentEntity.getListActions(actionFactory) should be (List(startCreateParent))
      childEntity.getListActions(actionFactory) should be (List(startCreateChild))
    }
  }

  it("should get the correct list actions with child entities w/ no parent display") {
    val persistence = mock[CrudPersistence]
    val actionFactory = mock[UIActionFactory]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    var parentEntity = new MyEntityType(persistence, listAdapter)
    val childEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val fields = foreignKey(parentEntity) :: super.fields
    }
    val childEntity2 = new MyEntityType(persistence, listAdapter) {
      override lazy val fields = foreignKey(parentEntity) :: super.fields
    }
    expecting {
      call(actionFactory.application).andReturn(application).anyTimes
      call(application.allEntities).andReturn(List(parentEntity, childEntity, childEntity2)).anyTimes
      call(actionFactory.startCreate(parentEntity)).andReturn(startCreateParent)
      call(actionFactory.startUpdate(parentEntity)).andReturn(startUpdateParent)
      call(actionFactory.adapt[Unit,ID](eql(startUpdateParent), notNull())).andReturn(adaptedStartUpdateParent)
      call(actionFactory.startCreate(childEntity)).andReturn(startCreateChild)
      call(actionFactory.displayList(childEntity)).andStubReturn(displayChildList)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChildList), notNull())).andStubReturn(adaptedDisplayChildList)
      call(actionFactory.displayList(childEntity2)).andReturn(displayChild2List)
      call(actionFactory.adapt[ID,EntityUriSegment](eql(displayChild2List), notNull())).andReturn(adaptedDisplayChild2List)
    }
    whenExecuting(actionFactory, persistence, application) {
      parentEntity.getListActions(actionFactory) should be (List(startCreateParent))
      childEntity.getListActions(actionFactory) should be (List(adaptedStartUpdateParent, adaptedDisplayChild2List, startCreateChild))
    }
  }

  it("should delete with undo possibility") {
    val persistence = mock[CrudPersistence]
    val actionFactory = mock[UIActionFactory]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    var entity = new MyEntityType(persistence, listAdapter)
    val readable = mutable.Map[String,Any]()
    val id = 345L
    expecting {
      call(actionFactory.withEntityPersistence(eql(entity), notNull[CrudPersistence => Unit]())).andAnswer(answer {
        val currentArguments = EasyMock.getCurrentArguments
        val f = currentArguments(1).asInstanceOf[CrudPersistence => Unit]
        f(persistence)
      })
      call(actionFactory.application).andReturn(application).anyTimes
      call(persistence.find(id)).andReturn(Some(readable))
      call(persistence.delete(List(id)))
      call(actionFactory.addUndoableDelete(eql(entity), notNull[Undoable[ID]])).andAnswer(answer {
        val currentArguments = EasyMock.getCurrentArguments
        val undoable = currentArguments(1).asInstanceOf[Undoable[ID]]
        undoable.close()
      })
    }
    whenExecuting(actionFactory, persistence, application) {
      entity.startDelete(id, actionFactory)
    }
  }

  it("undo of delete should work") {
    val persistence = mock[CrudPersistence]
    val actionFactory = mock[UIActionFactory]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    var entity = new MyEntityType(persistence, listAdapter)
    val readable = mutable.Map[String,Any]("name" -> "George")
    val id = 345L
    val id2 = 444L
    expecting {
      call(actionFactory.withEntityPersistence(eql(entity), notNull[CrudPersistence => Unit]())).andAnswer(answer {
        val currentArguments = EasyMock.getCurrentArguments
        val f = currentArguments(1).asInstanceOf[CrudPersistence => Unit]
        f(persistence)
      })
      call(actionFactory.application).andReturn(application).anyTimes
      call(persistence.find(id)).andReturn(Some(readable))
      call(persistence.delete(List(id)))
      call(persistence.save(None, mutable.Map("name" -> "George"))).andReturn(id2)
      call(actionFactory.addUndoableDelete(eql(entity), notNull[Undoable[ID]])).andAnswer(answer {
        val currentArguments = EasyMock.getCurrentArguments
        val undoable = currentArguments(1).asInstanceOf[Undoable[ID]]
        undoable.undo() should be(id2)
      })
    }
    whenExecuting(actionFactory, persistence, application) {
      entity.startDelete(id, actionFactory)
    }
  }
}