package geeks.crud.android

import android.widget.CursorAdapter
import geeks.crud.EntityPersistence
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

trait AndroidEntityPersistence[ID,Q,L,R,W] extends EntityPersistence[ID,Q,L,R,W] {
  def createListAdapter(activity: Activity): CursorAdapter
}