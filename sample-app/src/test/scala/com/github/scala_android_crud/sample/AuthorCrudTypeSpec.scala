package com.github.scala_android_crud.sample

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.EasyMockSugar
import com.github.scala_android.crud.CursorField._
import com.github.scala_android.crud._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * todo A ... 
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 5/21/11
 * Time: 9:17 PM
 */
@RunWith(classOf[JUnitRunner])
class AuthorCrudTypeSpec extends Spec with ShouldMatchers with EasyMockSugar {
  it("should calculate the book count") {
    val crudContext = mock[CrudContext]
    whenExecuting(crudContext) {
      val context = new AuthorContext {
        val bookPersistence = new ListBufferEntityPersistence[Map[String, Any]] {
          def entityType = AuthorCrudType
        }

        val BookCrudType = new HiddenEntityType with GeneratedCrudType[Map[String,Any]] {
          def entityName = "Book"
          val authorIdField = foreignKey(AuthorCrudType)
          def fields = List(authorIdField)
          def openEntityPersistence(crudContext: CrudContext) = {
            bookPersistence
          }
        }

        object AuthorCrudType extends AuthorCrudType with GeneratedCrudType[Map[String,Any]] {
          def openEntityPersistence(crudContext: CrudContext) = throw new UnsupportedOperationException
        }
      }
      context.BookCrudType.openEntityPersistence(null).buffer += Map.empty[String,Any] += Map.empty[String,Any]

      val authorData = context.AuthorCrudType.transformWithItem(Map.empty[String,Any], List(Map[String,Any](persistedId.name -> 100L), crudContext))
      authorData should be (Map[String,Any](persistedId.name -> 100L, "bookCount" -> 2))
    }
  }
}
