package com.github.scala_android.crud

import android.content.{ContentValues, Context}
import android.widget.ListAdapter
import res.R

trait GeneratedCrudType[T <: AnyRef,Q <: AnyRef] extends CrudEntityType[Q,List[T],T,ContentValues] {
  def newWritable = new ContentValues

  def openEntityPersistence(context: Context): ListEntityPersistence[T,Q]

  def refreshAfterSave(listAdapter: ListAdapter) {}

  override def getListActions(actionFactory: UIActionFactory) = foreignKeys match {
    //exactly one parent w/o a display page
    case foreignKey :: Nil if !foreignKey.entityType.hasDisplayPage => {
      val parentEntity = foreignKey.entityType
      val getForeignKey = { _: Unit => foreignKey.partialGet(actionFactory.currentIntent).get }
      actionFactory.adapt(actionFactory.startUpdate(parentEntity), getForeignKey) ::
              parentEntity.displayChildEntityLists(actionFactory, getForeignKey, childEntities(actionFactory.application))
    }
    case _ => Nil
  }

  override def getEntityActions(actionFactory: UIActionFactory): List[UIAction[ID]] =
    displayLayout.map(_ => actionFactory.display(this)).toList

  val cancelItemString = R.string.cancel_item
}




