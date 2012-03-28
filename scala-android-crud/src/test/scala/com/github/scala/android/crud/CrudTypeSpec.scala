package com.github.scala.android.crud

import action.ContextVars
import common.UriPath
import org.junit.runner.RunWith
import persistence.CursorField
import scala.collection.mutable
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.Spec
import org.mockito._
import Mockito._
import Matchers._
import com.github.triangle.PortableField._
import ParentField.foreignKey

/** A behavior specification for [[com.github.scala.android.crud.CrudType]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class CrudTypeSpec extends Spec with MustMatchers with CrudMockitoSugar {

  it("must force having an id field on subtypes") {
    val crudType = new MyEntityType {
      override def valueFields = List(mapField[String]("name"))
    }
    crudType.deepCollect {
      case f if f == crudType.UriPathId => Some(true)
    }.flatten must be (Seq(true))
  }

  it("must derive parent entities from ParentField fields") {
    val entityType1 = new MyEntityType()
    val entityType2 = new MyEntityType()
    val crudType3 = new MyCrudType(new MyEntityType {
      override val valueFields = ParentField(entityType1) +: ParentField(entityType2) +: super.valueFields
    })
    val crudType1 = new MyCrudType(entityType1)
    val crudType2 = new MyCrudType(entityType2)
    val application = MyCrudApplication(crudType1, crudType2, crudType3)
    crudType3.parentEntities(application) must be (List(crudType1, crudType2))
  }

  it("must derive parent entities from foreignKey fields") {
    val entityType1 = new MyEntityType()
    val entityType2 = new MyEntityType()
    val crudType3 = new MyCrudType(new MyEntityType {
      override val valueFields = foreignKey(entityType1) +: foreignKey(entityType2) +: super.valueFields
    })
    val crudType1 = new MyCrudType(entityType1)
    val crudType2 = new MyCrudType(entityType2)
    val application = MyCrudApplication(crudType1, crudType2, crudType3)
    crudType3.parentEntities(application) must be (List(crudType1, crudType2))
  }

  it("must get the correct entity actions with child entities") {
    val parentEntityType = new MyEntityType()
    val childCrudType = new MyCrudType(new MyEntityType {
      override lazy val valueFields = ParentField(parentEntityType) :: super.valueFields
    })
    val parentCrudType = new MyCrudType(parentEntityType)
    val application = MyCrudApplication(childCrudType, parentCrudType)
    childCrudType.getEntityActions(application) must be (List(childCrudType.updateAction.get, childCrudType.deleteAction.get))
    parentCrudType.getEntityActions(application) must be (
      List(childCrudType.listAction, parentCrudType.updateAction.get, parentCrudType.deleteAction.get))
  }

  it("must get the correct list actions with child entities") {
    val parentEntityType = new MyEntityType
    val childEntityType1 = new MyEntityType {
      override lazy val valueFields = ParentField(parentEntityType) :: super.valueFields
    }
    val childEntityType2 = new MyEntityType {
      override lazy val valueFields = ParentField(parentEntityType) :: super.valueFields
    }
    val parentCrudType = new MyCrudType(parentEntityType) {
      override lazy val displayLayout = Some(123)
    }
    val childCrudType1 = new MyCrudType(childEntityType1)
    val childCrudType2 = new MyCrudType(childEntityType2)
    val application = MyCrudApplication(childCrudType1, childCrudType2, parentCrudType)
    parentCrudType.getListActions(application) must be (List(parentCrudType.createAction.get))
    childCrudType1.getListActions(application) must be (List(childCrudType1.createAction.get))
  }

  it("must get the correct list actions with child entities w/ no parent display") {
    val parentEntityType = new MyEntityType
    val childEntityType1 = new MyEntityType {
      override lazy val valueFields = ParentField(parentEntityType) :: super.valueFields
    }
    val childEntityType2 = new MyEntityType {
      override lazy val valueFields = ParentField(parentEntityType) :: super.valueFields
    }
    val parentCrudType = new MyCrudType(parentEntityType)
    val childCrudType1 = new MyCrudType(childEntityType1)
    val childCrudType2 = new MyCrudType(childEntityType2)
    val application = MyCrudApplication(parentCrudType, childCrudType1, childCrudType2)
    parentCrudType.getListActions(application) must be (List(parentCrudType.createAction.get))
    childCrudType1.getListActions(application) must be (
      List(parentCrudType.updateAction.get, childCrudType2.listAction, childCrudType1.createAction.get))
  }

  it("must delete with undo possibility which must be closable") {
    val persistence = mock[CrudPersistence]
    val activity = mock[CrudActivity]
    val crudContext = mock[CrudContext]
    stub(crudContext.vars).toReturn(new ContextVars {})
    val entity = new MyCrudType(persistence)
    val readable = mutable.Map[String,Any]()
    val uri = UriPath(entity.entityName) / 345L
    stub(activity.crudContext).toReturn(crudContext)
    stub(persistence.crudContext).toReturn(crudContext)
    stub(persistence.find(uri)).toReturn(Some(readable))
    stub(activity.allowUndo(notNull.asInstanceOf[Undoable])).toAnswer(answerWithInvocation { invocationOnMock =>
      val currentArguments = invocationOnMock.getArguments
      val undoable = currentArguments(0).asInstanceOf[Undoable]
      undoable.closeOperation.foreach(_.invoke(uri, activity))
    })
    entity.startDelete(uri, activity)
    verify(persistence).delete(uri)
  }

  it("undo of delete must work") {
    val persistence = mock[CrudPersistence]
    val activity = mock[CrudActivity]
    val crudContext = mock[CrudContext]
    val entity = new MyCrudType(persistence)
    val readable = mutable.Map[String,Any](CursorField.idFieldName -> 345L, "name" -> "George")
    val uri = UriPath(entity.entityName) / 345L
    stub(activity.crudContext).toReturn(crudContext)
    val vars = new ContextVars {}
    stub(crudContext.vars).toReturn(vars)
    stub(crudContext.context).toReturn(activity)
    stub(crudContext.application).toReturn(MyCrudApplication(entity))
    stub(activity.variables).toReturn(vars.variables)
    stub(persistence.crudContext).toReturn(crudContext)
    stub(persistence.find(uri)).toReturn(Some(readable))
    when(activity.allowUndo(notNull.asInstanceOf[Undoable])).thenAnswer(answerWithInvocation { invocationOnMock =>
      val currentArguments = invocationOnMock.getArguments
      val undoable = currentArguments(0).asInstanceOf[Undoable]
      undoable.undoAction.invoke(uri, activity)
    })
    entity.startDelete(uri, activity)
    verify(persistence).delete(uri)
    verify(persistence).save(Some(345L), mutable.Map(CursorField.idFieldName -> 345L, "name" -> "George"))
  }
}
