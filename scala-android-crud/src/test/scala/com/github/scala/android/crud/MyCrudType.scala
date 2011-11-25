package com.github.scala.android.crud

import com.github.triangle.BaseField
import android.widget.ListAdapter
import persistence.CursorField.persisted

/**
 * A simple CrudType for testing.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/15/11
 * Time: 10:40 PM
 */
object MyCrudType extends MyCrudType

class MyCrudType extends CrudType with StubEntityType {
  def entityName = "MyEntity"

  def valueFields = List[BaseField](persisted[Int]("count"))

  def refreshAfterDataChanged(listAdapter: ListAdapter) {}

  def setListAdapter(findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity) {
    throw new UnsupportedOperationException
  }

  protected def createEntityPersistence(crudContext: CrudContext): CrudPersistence = throw new UnsupportedOperationException

  def newWritable: AnyRef = throw new UnsupportedOperationException
}

trait StubEntityType extends CrudType {
  override lazy val entityNameLayoutPrefix = "test"

  def listActivityClass = classOf[CrudListActivity]
  def activityClass = classOf[CrudActivity]
}
