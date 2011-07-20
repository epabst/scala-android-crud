package com.github.scala_android_crud.sample

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.EasyMockSugar
import com.github.scala_android.crud.CursorField._
import com.github.scala_android.crud._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * A behavior specification for {@link AuthorCrudType}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 5/21/11
 * Time: 9:17 PM
 */
@RunWith(classOf[JUnitRunner])
class AuthorCrudTypeSpec extends Spec with MustMatchers with EasyMockSugar {
  it("must have the right children") {
    SampleApplication.AuthorCrudType.childEntities(SampleApplication) must
            be (List[CrudType](SampleApplication.BookCrudType))
  }

  it("must calculate the book count") {
    val crudContext = mock[CrudContext]
    whenExecuting(crudContext) {
      val context = new AuthorContext {
        val bookPersistence: ListBufferEntityPersistence[Map[String,Any]] = new ListBufferEntityPersistence[Map[String, Any]] {
          def entityType = BookCrudType
        }

        val BookCrudType = new HiddenEntityType with GeneratedCrudType[Map[String,Any]] {
          def entityName = "Book"
          val authorIdField = foreignKey(AuthorCrudType)
          def fields = List(authorIdField)
          def openEntityPersistence(crudContext: CrudContext) = bookPersistence
        }

        object AuthorCrudType extends AuthorCrudType with GeneratedCrudType[Map[String,Any]] {
          def openEntityPersistence(crudContext: CrudContext) = throw new UnsupportedOperationException
        }
      }
      context.bookPersistence.buffer += Map.empty[String,Any] += Map.empty[String,Any]

      val authorData = context.AuthorCrudType.transformWithItem(Map.empty[String,Any], List(Map[String,Any](persistedId.name -> 100L), crudContext))
      authorData must be (Map[String,Any](persistedId.name -> 100L, "bookCount" -> 2))
    }
  }
}
