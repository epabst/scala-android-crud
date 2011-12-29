package com.github.scala.android.crud.sample

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import com.github.scala.android.crud.persistence.CursorField._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import com.github.scala.android.crud._
import action.{ContextWithVars, ContextVars}

/** A behavior specification for [[com.github.scala.android.crud.sample.AuthorCrudType]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class AuthorCrudTypeSpec extends Spec with MustMatchers with MockitoSugar {
  it("must have the right children") {
    SampleApplication.AuthorCrudType.childEntities(SampleApplication) must
            be (List[CrudType](SampleApplication.BookCrudType))
  }

  it("must calculate the book count") {
    val contextVars = new ContextVars {}
    val application = mock[CrudApplication]
    val crudContext = new CrudContext(mock[ContextWithVars], application) {
      override def vars = contextVars
    }
    val factory = GeneratedPersistenceFactory(new ListBufferCrudPersistence(Map.empty[String, Any], _, crudContext))
    val bookCrudType = new GeneratedCrudType[Map[String,Any]](BookEntityType, factory) with HiddenCrudType
    val bookPersistence = bookCrudType.openEntityPersistence(crudContext).asInstanceOf[ListBufferCrudPersistence[Map[String,Any]]]
    bookPersistence.buffer += Map.empty[String,Any] += Map.empty[String,Any]

    stub(application.crudType(BookEntityType)).toReturn(bookCrudType)
    val authorData = AuthorEntityType.transformWithItem(Map.empty[String,Any], List(AuthorEntityType.toUri(100L), crudContext))
    authorData must be (Map[String,Any](idFieldName -> 100L, "bookCount" -> 2))
  }
}
