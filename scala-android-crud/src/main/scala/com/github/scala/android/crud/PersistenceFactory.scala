package com.github.scala.android.crud

import persistence.EntityType
import android.widget.ListAdapter

/**
 * A factory for EntityPersistence specific to a storage type such as SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 12/6/11
 * Time: 10:05 PM
 */

trait PersistenceFactory {
  /**
   * Instantiates a data buffer which can be saved by EntityPersistence.
   * The EntityType must support copying into this object.
   */
  def newWritable: AnyRef

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext): CrudPersistence

  def setListAdapter(crudType: CrudType, findAllResult: Seq[AnyRef], contextItems: List[AnyRef], activity: CrudListActivity)

  def refreshAfterDataChanged(listAdapter: ListAdapter)
}
