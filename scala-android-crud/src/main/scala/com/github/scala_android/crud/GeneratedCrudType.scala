package com.github.scala_android.crud

import com.github.scala_android.crud
import android.content.{ContentValues, Context}
import android.widget.ListAdapter

trait GeneratedCrudType[T <: AnyRef,Q <: AnyRef] extends CrudEntityType[Q,List[T],T,ContentValues] {
  def childEntities = Nil

  def openEntityPersistence(context: Context): ListEntityPersistence[T,Q]

  def refreshAfterSave(listAdapter: ListAdapter) {}

  override def getListActions(actionFactory: UIActionFactory) = foreignKeys match {
    //exactly one parent w/o a display page
    case foreignKey :: Nil if !foreignKey.entityType.hasDisplayPage => {
      val parentEntity = foreignKey.entityType
      val foreignId = foreignKey.partialGet(actionFactory.currentIntent).get
      actionFactory.startUpdate(parentEntity, foreignId) :: parentEntity.displayChildEntityLists(actionFactory, foreignId)
    }
    case _ => Nil
  }

  override def getEntityActions(actionFactory: UIActionFactory, id: ID) = List()

  val listLayout = R.layout.entity_list
  val cancelItemString = crud.R.string.cancel_item
}




