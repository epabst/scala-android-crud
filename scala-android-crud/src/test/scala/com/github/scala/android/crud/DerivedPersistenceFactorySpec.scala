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
    val persistence1 = mock[CrudPersistence]
    val persistence2 = mock[CrudPersistence]
    val crudType1 = new MyCrudType(persistence1)
    val crudType2 = new MyCrudType(persistence2)
    val factory = new DerivedPersistenceFactory[String](crudType1, crudType2) {
      def findAll(entityType: EntityType, uri: UriPath, delegatePersistenceMap: Map[EntityType, CrudPersistence]) = {
        delegatePersistenceMap must be (Map(crudType1 -> persistence1, crudType2 -> persistence2))
        List("findAll", "was", "called")
      }
    }
    val crudContext = mock[CrudContext]
    stub(crudContext.vars).toReturn(new ContextVars {})
    val persistence = factory.createEntityPersistence(mock[EntityType], crudContext)
    persistence.findAll(UriPath()) must be (List("findAll", "was", "called"))
  }

  it("must close each delegate CrudPersistence when close is called") {
    val persistence1 = mock[CrudPersistence]
    val persistence2 = mock[CrudPersistence]
    val factory = new DerivedPersistenceFactory[String](new MyCrudType(persistence1), new MyCrudType(persistence2)) {
      def findAll(entityType: EntityType, uri: UriPath, delegatePersistenceMap: Map[EntityType, CrudPersistence]) = Nil
    }
    val crudContext = mock[CrudContext]
    stub(crudContext.vars).toReturn(new ContextVars {})
    val persistence = factory.createEntityPersistence(mock[EntityType], crudContext)
    persistence.close()
    verify(persistence1).close()
    verify(persistence2).close()
  }
}