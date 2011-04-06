package com.github.scala_android.crud

import android.content.ContentValues
import res.R
import android.widget.BaseAdapter
import android.view.{ViewGroup, View}

trait GeneratedCrudType[T <: AnyRef,Q <: AnyRef] extends CrudEntityType[Q,List[T],T,ContentValues] {
  def newWritable = new ContentValues

  def openEntityPersistence(crudContext: CrudContext): ListEntityPersistence[T,Q]

  def createListAdapter(persistence: EntityPersistence[Q,List[T],T,ContentValues], crudContext: CrudContext) = new BaseAdapter() {
    val listPersistence = persistence.asInstanceOf[ListEntityPersistence[T, Q]]
    val list = listPersistence.list

    def getCount: Int = list.size

    def getItemId(position: Int) = listPersistence.getId(list(position))

    def getItem(position: Int) = list(position)

    def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val view = if (convertView == null) crudContext.activity.getLayoutInflater.inflate(rowLayout, parent, false) else convertView
      copyFields(list(position), view)
      view
    }
  }

  def refreshAfterSave(crudContext: CrudContext) {}

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




