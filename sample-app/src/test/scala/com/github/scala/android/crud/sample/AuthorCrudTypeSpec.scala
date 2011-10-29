package com.github.scala.android.crud.sample

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.EasyMockSugar
import com.github.scala.android.crud.persistence.CursorField._
import com.github.scala.android.crud._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.scala.android.crud.ParentField._

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
        val BookCrudType = new HiddenEntityType with GeneratedCrudType[Map[String,Any]] {
          val bookPersistence = new ListBufferCrudPersistence[Map[String, Any]](this, crudContext)
          def entityName = "Book"
          def valueFields = List(foreignKey(AuthorCrudType))
          protected def createEntityPersistence(crudContext: CrudContext) = bookPersistence
        }

        object AuthorCrudType extends AuthorCrudType with GeneratedCrudType[Map[String,Any]] {
          protected def createEntityPersistence(crudContext: CrudContext) = throw new UnsupportedOperationException
        }
      }
      context.BookCrudType.bookPersistence.buffer += Map.empty[String,Any] += Map.empty[String,Any]

      val authorData = context.AuthorCrudType.transformWithItem(Map.empty[String,Any], List(Map[String,Any](idFieldName -> 100L), crudContext))
      authorData must be (Map[String,Any](idFieldName -> 100L, "bookCount" -> 2))
    }
  }
}
