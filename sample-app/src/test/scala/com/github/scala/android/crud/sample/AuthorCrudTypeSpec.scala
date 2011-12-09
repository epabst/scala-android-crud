package com.github.scala.android.crud.sample

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import com.github.scala.android.crud.persistence.CursorField._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.scala.android.crud.ParentField._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import com.github.scala.android.crud._
import action.ContextVars

/**
 * A behavior specification for {@link AuthorCrudType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 5/21/11
 * Time: 9:17 PM
 */
@RunWith(classOf[JUnitRunner])
class AuthorCrudTypeSpec extends Spec with MustMatchers with MockitoSugar {
  it("must have the right children") {
    SampleApplication.AuthorCrudType.childEntities(SampleApplication) must
            be (List[CrudType](SampleApplication.BookCrudType))
  }

  it("must calculate the book count") {
    val crudContext = mock[CrudContext]
    stub(crudContext.vars).toReturn(new ContextVars {})
    val factory = GeneratedPersistenceFactory(new ListBufferCrudPersistence[Map[String, Any]](_, crudContext))
    val context = new AuthorContext {
      val BookCrudType = new GeneratedCrudType[Map[String,Any]](factory) with HiddenEntityType {
        def entityName = "Book"
        def valueFields = List(foreignKey(AuthorCrudType))
      }

      object AuthorCrudType extends AuthorCrudType(mock[PersistenceFactory])
    }
    val bookPersistence = context.BookCrudType.openEntityPersistence(crudContext).asInstanceOf[ListBufferCrudPersistence[Map[String,Any]]]
    bookPersistence.buffer += Map.empty[String,Any] += Map.empty[String,Any]

    val crudType = context.AuthorCrudType
    val authorData = crudType.transformWithItem(Map.empty[String,Any], List(crudType.toUri(100L), crudContext))
    authorData must be (Map[String,Any](idFieldName -> 100L, "bookCount" -> 2))
  }
}
