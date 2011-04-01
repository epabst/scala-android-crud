package com.github.scala_android.crud

import android.widget.ListAdapter
import android.app.Activity

/**
 * Persistence support for an entity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 4:12 PM
 * @param ID the ID type for the entity such as String or Long.
 * @param Q the query criteria type
 * @param L the type of findAll (e.g. Cursor)
 * @param R the type to read from (e.g. Cursor)
 * @param W the type to write to (e.g. ContentValues)
 */

trait EntityPersistence[Q,L,R,W] extends PlatformTypes {
  def newCriteria: Q

  def findAll(query: Q): L

  def createListAdapter(activity: Activity): ListAdapter

  /** Find an entity by ID. */
  def find(id: ID): Option[R]

  def newWritable: W

  /** Save a created or updated entity. */
  def save(id: Option[ID], writable: W): ID

  /** Delete a list of entities by ID. */
  def delete(ids: List[ID])

  def close()
}