package com.github.scala_android.crud

import _root_.android.widget.TextView
import org.scalatest.mock.EasyMockSugar
import scala.collection.mutable.Map
import com.github.triangle._
import Field._
import ViewFieldAccess._
import CursorFieldAccess._
import android.content.{Intent, Context}
import android.database.Cursor
import org.easymock.{IAnswer, EasyMock}

/**
 * An object mother pattern for getting CrudEntityConfig instances.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/26/11
 * Time: 11:06 PM
 */

trait MyEntityTesting extends EasyMockSugar {
  class MyEntityConfig(persistence: EntityPersistence[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]]) extends CrudEntityConfig[AnyRef,List[Map[String,Any]],Map[String,Any],Map[String,Any]] {
    val entityName = "MyMap"

    def fields = List(
      Field(persisted[String]("name"), viewId[TextView,String](R.id.name)),
      Field(persisted[Long]("age"), viewId[TextView,Long](R.id.age)),
      //here to test a non-UI field
      Field[String](persisted("uri"), readOnly[Intent,String](_.getData.toString)))

    def openEntityPersistence(context: Context) = persistence

    val listLayout = R.layout.entity_list
    val headerLayout = R.layout.test_row
    val rowLayout = R.layout.test_row
    val entryLayout = R.layout.test_entry
    val addItemString = R.string.add_item
    val editItemString = R.string.edit_item
    val cancelItemString = R.string.cancel_item

    def listActivityClass = classOf[CrudListActivity[_,_,_,_]]
    def activityClass = classOf[CrudActivity[_,_,_,_]]
  }

  def mockCursor(cursorValues: Map[String,Option[Any]]) = {
    val cursor = mock[Cursor]
    val cursorColumnNames = cursorValues.keys.toList
    import EasyMock._
    call(cursor.getColumnIndex(isA(classOf[String]))).andAnswer(answer(cursorColumnNames.indexOf(getCurrentArguments.apply(0).asInstanceOf[String]))).anyTimes
    def currentColumnName = cursorColumnNames.apply(getCurrentArguments.apply(0).asInstanceOf[Int])
    call(cursor.isNull(anyInt)).andAnswer(answer(cursorValues(currentColumnName).isEmpty)).anyTimes
    call(cursor.getLong(anyInt)).andAnswer(answer(cursorValues(currentColumnName).get.asInstanceOf[Long])).anyTimes
    call(cursor.getDouble(anyInt)).andAnswer(answer(cursorValues(currentColumnName).get.asInstanceOf[Double])).anyTimes
    call(cursor.getInt(anyInt)).andAnswer(answer(cursorValues(currentColumnName).get.asInstanceOf[Int])).anyTimes
    call(cursor.getString(anyInt)).andAnswer(answer(cursorValues(currentColumnName).get.asInstanceOf[String])).anyTimes
    cursor
  }

  def answer[T](result: => T) = new IAnswer[T] {
    def answer = result
  }
}