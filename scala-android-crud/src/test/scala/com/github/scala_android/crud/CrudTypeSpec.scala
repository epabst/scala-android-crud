package com.github.scala_android.crud

import org.junit.runner.RunWith
import persistence.{IdPk, CrudPersistence}
import scala.collection.mutable
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import persistence.CursorField._
import org.easymock.EasyMock
import EasyMock.notNull
import android.widget.ListAdapter
import com.github.triangle.PortableField._

/**
 * A behavior specification for {@link CrudType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/22/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class CrudTypeSpec extends Spec with MustMatchers with MyEntityTesting with CrudEasyMockSugar {

  it("must force having an id field on subtypes") {
    val crudType = new MyEntityType(null, null) {
      override def valueFields = List(mapField[String]("name"))
    }
    crudType.deepCollect {
      case f if f == IdPk.idField => Some(true)
    }.flatten must be (Seq(true))
  }

  it("must derive parent entities from foreignKey fields") {
    val persistence = mock[CrudPersistence]
    val listAdapter = mock[ListAdapter]
    whenExecuting(persistence, listAdapter) {
      val entityType1 = new MyEntityType(persistence, listAdapter)
      val entityType2 = new MyEntityType(persistence, listAdapter)
      val entityType3 = new MyEntityType(persistence, listAdapter) {
        override val valueFields = ForeignKey(entityType1) +: ForeignKey(entityType2) +: super.valueFields
      }
      entityType3.parentEntities must be (List(entityType1, entityType2))
    }
  }

  it("must get the correct entity actions with child entities") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val parentEntity = new MyEntityType(persistence, listAdapter)
    val childEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ForeignKey(parentEntity) :: super.valueFields
    }
    expecting {
      call(application.allEntities).andReturn(List(parentEntity, childEntity)).anyTimes
    }
    whenExecuting(persistence, application) {
      childEntity.getEntityActions(application) must be (List(childEntity.updateAction, childEntity.deleteAction))
      parentEntity.getEntityActions(application) must be (List(childEntity.listAction, parentEntity.updateAction, parentEntity.deleteAction))
    }
  }

  it("must get the correct list actions with child entities") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val parentEntity = new MyEntityType(persistence, listAdapter) {
      override val displayLayout = Some(123)
    }
    val childEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ForeignKey(parentEntity) :: super.valueFields
    }
    val childEntity2 = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ForeignKey(parentEntity) :: super.valueFields
    }
    expecting {
      call(application.allEntities).andReturn(List(parentEntity, childEntity, childEntity2)).anyTimes
    }
    whenExecuting(persistence, application) {
      parentEntity.getListActions(application) must be (List(parentEntity.createAction))
      childEntity.getListActions(application) must be (List(childEntity.createAction))
    }
  }

  it("must get the correct list actions with child entities w/ no parent display") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    var parentEntity = new MyEntityType(persistence, listAdapter)
    val childEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ForeignKey(parentEntity) :: super.valueFields
    }
    val childEntity2 = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ForeignKey(parentEntity) :: super.valueFields
    }
    expecting {
      call(application.allEntities).andReturn(List(parentEntity, childEntity, childEntity2)).anyTimes
    }
    whenExecuting(persistence, application) {
      parentEntity.getListActions(application) must be (List(parentEntity.createAction))
      childEntity.getListActions(application) must be (List(parentEntity.updateAction, childEntity2.listAction, childEntity.createAction))
    }
  }

  it("must delete with undo possibility") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val activity = mock[BaseCrudActivity]
    var entity = new MyEntityType(persistence, listAdapter)
    val readable = mutable.Map[String,Any]()
    val id = 345L
    expecting {
      call(persistence.find(id)).andReturn(Some(readable))
      call(persistence.delete(List(id)))
      call(activity.addUndoableDelete(eql(entity), notNull[Undoable[ID]])).andAnswer(answer {
        val currentArguments = EasyMock.getCurrentArguments
        val undoable = currentArguments(1).asInstanceOf[Undoable[ID]]
        undoable.close()
      })
    }
    whenExecuting(activity, persistence, application) {
      entity.startDelete(id, activity)
    }
  }

  it("undo of delete must work") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val activity = mock[CrudActivity]
    val listAdapter = mock[ListAdapter]
    var entity = new MyEntityType(persistence, listAdapter)
    val readable = mutable.Map[String,Any]("name" -> "George")
    val id = 345L
    val id2 = 444L
    expecting {
      call(persistence.find(id)).andReturn(Some(readable))
      call(persistence.delete(List(id)))
      call(persistence.save(None, mutable.Map("name" -> "George"))).andReturn(id2)
      call(activity.addUndoableDelete(eql(entity), notNull[Undoable[ID]])).andAnswer(answer {
        val currentArguments = EasyMock.getCurrentArguments
        val undoable = currentArguments(1).asInstanceOf[Undoable[ID]]
        undoable.undo() must be(id2)
      })
    }
    whenExecuting(activity, persistence, application) {
      entity.startDelete(id, activity)
    }
  }
}