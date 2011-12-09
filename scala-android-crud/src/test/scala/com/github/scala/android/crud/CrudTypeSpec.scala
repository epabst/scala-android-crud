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

/**
 * A behavior specification for {@link CrudType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/22/11
 * Time: 7:59 PM
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
    val crudType1 = new MyCrudType()
    val crudType2 = new MyCrudType()
    val crudType3 = new MyCrudType() {
      override val valueFields = ParentField(crudType1) +: ParentField(crudType2) +: super.valueFields
    }
    crudType3.parentEntities must be (List(crudType1, crudType2))
  }

  it("must derive parent entities from foreignKey fields") {
    val crudType1 = new MyCrudType()
    val crudType2 = new MyCrudType()
    val crudType3 = new MyCrudType() {
      override val valueFields = foreignKey(crudType1) +: foreignKey(crudType2) +: super.valueFields
    }
    crudType3.parentEntities must be (List(crudType1, crudType2))
  }

  it("must get the correct entity actions with child entities") {
    val application = mock[CrudApplication]
    val parentCrudType = new MyCrudType()
    val childCrudType = new MyCrudType() {
      override lazy val valueFields = ParentField(parentCrudType) :: super.valueFields
    }
    stub(application.allEntities).toReturn(List(parentCrudType, childCrudType))
    childCrudType.getEntityActions(application) must be (List(childCrudType.updateAction.get, childCrudType.deleteAction.get))
    parentCrudType.getEntityActions(application) must be (
      List(childCrudType.listAction, parentCrudType.updateAction.get, parentCrudType.deleteAction.get))
  }

  it("must get the correct list actions with child entities") {
    val application = mock[CrudApplication]
    val parentEntity = new MyCrudType() {
      override lazy val displayLayout = Some(123)
    }
    val childEntity = new MyCrudType() {
      override lazy val valueFields = ParentField(parentEntity) :: super.valueFields
    }
    val childEntity2 = new MyCrudType() {
      override lazy val valueFields = ParentField(parentEntity) :: super.valueFields
    }
    stub(application.allEntities).toReturn(List(parentEntity, childEntity, childEntity2))
    parentEntity.getListActions(application) must be (List(parentEntity.createAction.get))
    childEntity.getListActions(application) must be (List(childEntity.createAction.get))
  }

  it("must get the correct list actions with child entities w/ no parent display") {
    val application = mock[CrudApplication]
    val parentEntity = new MyCrudType()
    val childEntity = new MyCrudType() {
      override lazy val valueFields = ParentField(parentEntity) :: super.valueFields
    }
    val childEntity2 = new MyCrudType() {
      override lazy val valueFields = ParentField(parentEntity) :: super.valueFields
    }
    stub(application.allEntities).toReturn(List(parentEntity, childEntity, childEntity2))
    parentEntity.getListActions(application) must be (List(parentEntity.createAction.get))
    childEntity.getListActions(application) must be (
      List(parentEntity.updateAction.get, childEntity2.listAction, childEntity.createAction.get))
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
