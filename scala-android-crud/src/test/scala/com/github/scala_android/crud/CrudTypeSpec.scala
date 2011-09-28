package com.github.scala_android.crud

import org.junit.runner.RunWith
import persistence.IdPk
import scala.collection.mutable
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import persistence.CursorField._
import org.mockito._
import Mockito._
import Matchers._
import android.widget.ListAdapter
import com.github.triangle.PortableField._
import ParentField.foreignKey

/**
 * A behavior specification for {@link CrudType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/22/11
 * Time: 7:59 PM
 */

@RunWith(classOf[JUnitRunner])
class CrudTypeSpec extends Spec with MustMatchers with MyEntityTesting with CrudMockitoSugar {

  it("must force having an id field on subtypes") {
    val crudType = new MyEntityType(null, null) {
      override def valueFields = List(mapField[String]("name"))
    }
    crudType.deepCollect {
      case f if f == IdPk.idField => Some(true)
    }.flatten must be (Seq(true))
  }

  it("must derive parent entities from ParentField fields") {
    val persistence = mock[CrudPersistence]
    val listAdapter = mock[ListAdapter]
    val entityType1 = new MyEntityType(persistence, listAdapter)
    val entityType2 = new MyEntityType(persistence, listAdapter)
    val entityType3 = new MyEntityType(persistence, listAdapter) {
      override val valueFields = ParentField(entityType1) +: ParentField(entityType2) +: super.valueFields
    }
    entityType3.parentEntities must be (List(entityType1, entityType2))
  }

  it("must derive parent entities from foreignKey fields") {
    val persistence = mock[CrudPersistence]
    val listAdapter = mock[ListAdapter]
    val entityType1 = new MyEntityType(persistence, listAdapter)
    val entityType2 = new MyEntityType(persistence, listAdapter)
    val entityType3 = new MyEntityType(persistence, listAdapter) {
      override val valueFields = foreignKey(entityType1) +: foreignKey(entityType2) +: super.valueFields
    }
    entityType3.parentEntities must be (List(entityType1, entityType2))
  }

  it("must get the correct entity actions with child entities") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val parentEntity = new MyEntityType(persistence, listAdapter)
    val childEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ParentField(parentEntity) :: super.valueFields
    }
    stub(application.allEntities).toReturn(List(parentEntity, childEntity))
    childEntity.getEntityActions(application) must be (List(childEntity.updateAction.get, childEntity.deleteAction.get))
    parentEntity.getEntityActions(application) must be (
      List(childEntity.listAction, parentEntity.updateAction.get, parentEntity.deleteAction.get))
  }

  it("must get the correct list actions with child entities") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val parentEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val displayLayout = Some(123)
    }
    val childEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ParentField(parentEntity) :: super.valueFields
    }
    val childEntity2 = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ParentField(parentEntity) :: super.valueFields
    }
    stub(application.allEntities).toReturn(List(parentEntity, childEntity, childEntity2))
    parentEntity.getListActions(application) must be (List(parentEntity.createAction.get))
    childEntity.getListActions(application) must be (List(childEntity.createAction.get))
  }

  it("must get the correct list actions with child entities w/ no parent display") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    var parentEntity = new MyEntityType(persistence, listAdapter)
    val childEntity = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ParentField(parentEntity) :: super.valueFields
    }
    val childEntity2 = new MyEntityType(persistence, listAdapter) {
      override lazy val valueFields = ParentField(parentEntity) :: super.valueFields
    }
    stub(application.allEntities).toReturn(List(parentEntity, childEntity, childEntity2))
    parentEntity.getListActions(application) must be (List(parentEntity.createAction.get))
    childEntity.getListActions(application) must be (
      List(parentEntity.updateAction.get, childEntity2.listAction, childEntity.createAction.get))
  }

  it("must delete with undo possibility") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val listAdapter = mock[ListAdapter]
    val activity = mock[CrudActivity]
    val crudContext = new CrudContext(activity, application)
    var entity = new MyEntityType(persistence, listAdapter)
    val readable = mutable.Map[String,Any]()
    val id = 345L
    stub(activity.crudContext).toReturn(crudContext)
    stub(persistence.find(id)).toReturn(Some(readable))
    stub(activity.addUndoableDelete(eql(entity), notNull.asInstanceOf[Undoable[ID]])).toAnswer(answerWithInvocation { invocationOnMock =>
      val currentArguments = invocationOnMock.getArguments
      val undoable = currentArguments(1).asInstanceOf[Undoable[ID]]
      undoable.close()
    })
    entity.startDelete(id, activity)
    verify(persistence).delete(List(id))
  }

  it("undo of delete must work") {
    val persistence = mock[CrudPersistence]
    val application = mock[CrudApplication]
    val activity = mock[CrudActivity]
    val crudContext = new CrudContext(activity, application)
    val listAdapter = mock[ListAdapter]
    var entity = new MyEntityType(persistence, listAdapter)
    val readable = mutable.Map[String,Any]("name" -> "George")
    val id = 345L
    val id2 = 444L
    stub(activity.crudContext).toReturn(crudContext)
    stub(persistence.find(id)).toReturn(Some(readable))
    when(persistence.save(None, mutable.Map("name" -> "George"))).thenReturn(id2)
    when(activity.addUndoableDelete(eql(entity), notNull.asInstanceOf[Undoable[ID]])).thenAnswer(answerWithInvocation { invocationOnMock =>
      val currentArguments = invocationOnMock.getArguments
      val undoable = currentArguments(1).asInstanceOf[Undoable[ID]]
      undoable.undo() must be(id2)
    })
    entity.startDelete(id, activity)
    verify(persistence).delete(List(id))
  }
}
