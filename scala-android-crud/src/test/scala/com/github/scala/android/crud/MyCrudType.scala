package com.github.scala.android.crud

import com.github.triangle.BaseField
import persistence.CursorField.persisted
import org.mockito.Mockito

/**
 * A simple CrudType for testing.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/15/11
 * Time: 10:40 PM
 */
object MyCrudType extends MyCrudType(Mockito.mock(classOf[PersistenceFactory]))

class MyCrudType(persistenceFactory: PersistenceFactory) extends PersistedCrudType(persistenceFactory) with StubEntityType {
  def entityName = "MyEntity"

  def valueFields = List[BaseField](persisted[Int]("count"))
}

trait StubEntityType extends CrudType {
  override lazy val entityNameLayoutPrefix = "test"

  def listActivityClass = classOf[CrudListActivity]
  def activityClass = classOf[CrudActivity]
}
