package com.github.scala_android.crud

import res.R
import com.github.triangle.BaseField

/**
 * A simple CrudTypeRef for testing.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/15/11
 * Time: 10:40 PM
 */
object MyCrudTypeRef extends CrudTypeRef {
  def entityName = "MyEntity"

  def fields = List[BaseField]()

  def addItemString = R.string.add_item

  def editItemString = R.string.edit_item

  def cancelItemString = R.string.cancel_item

  def activityClass = classOf[CrudActivity]

  def listActivityClass = classOf[CrudListActivity]

  def hasDisplayPage = false
}
