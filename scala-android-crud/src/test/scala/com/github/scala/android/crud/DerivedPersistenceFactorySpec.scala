package com.github.scala.android.crud

import action.ContextVars
import common.UriPath
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import persistence.EntityType
import org.mockito.Mockito._

/**
 * A specification for [[com.github.scala.android.crud.DerivedPersistenceFactory]].
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 12/9/11
 * Time: 6:44 AM
 */
@RunWith(classOf[JUnitRunner])
class DerivedPersistenceFactorySpec extends Spec with MustMatchers with CrudMockitoSugar {
  it("must instantiate the CrudPersistence for the delegate CrudTypes and make them available") {
    val entityType1 = new MyEntityType
    val entityType2 = new MyEntityType
    val persistence1 = mock[CrudPersistence]
    val persistence2 = mock[CrudPersistence]
    val factory = new DerivedPersistenceFactory[String](entityType1, entityType2) {
      def findAll(entityType: EntityType, uri: UriPath, delegatePersistenceMap: Map[EntityType, CrudPersistence]) = {
        delegatePersistenceMap must be (Map(entityType1 -> persistence1, entityType2 -> persistence2))
        List("findAll", "was", "called")
      }
    }
    val crudContext = mock[CrudContext]
    stub(crudContext.vars).toReturn(new ContextVars {})
    when(crudContext.openEntityPersistence(entityType1)).thenReturn(persistence1)
    when(crudContext.openEntityPersistence(entityType2)).thenReturn(persistence2)
    val persistence = factory.createEntityPersistence(mock[EntityType], crudContext)
    persistence.findAll(UriPath()) must be (List("findAll", "was", "called"))
  }

  it("must close each delegate CrudPersistence when close is called") {
    val entityType1 = mock[EntityType]
    val entityType2 = mock[EntityType]
    val factory = new DerivedPersistenceFactory[String](entityType1, entityType2) {
      def findAll(entityType: EntityType, uri: UriPath, delegatePersistenceMap: Map[EntityType, CrudPersistence]) = Nil
    }
    val crudContext = mock[CrudContext]
    stub(crudContext.vars).toReturn(new ContextVars {})
    val persistence1 = mock[CrudPersistence]
    val persistence2 = mock[CrudPersistence]
    when(crudContext.openEntityPersistence(entityType1)).thenReturn(persistence1)
    when(crudContext.openEntityPersistence(entityType2)).thenReturn(persistence2)
    val persistence = factory.createEntityPersistence(mock[EntityType], crudContext)
    persistence.close()
    verify(persistence1).close()
    verify(persistence2).close()
  }
}