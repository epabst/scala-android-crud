package geeks.crud.android

import android.app.Activity
import android.content.Intent
import android.net.Uri

/**
 * Support for the different Crud Activity's.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/25/11
 * Time: 7:01 PM
 */

trait CrudContext[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef] { this: Activity =>
  def entityConfig: CrudEntityConfig[Q,L,R,W]

  private val activity: Activity = this

  val persistence: EntityPersistence[Q,L,R,W] = entityConfig.getEntityPersistence(activity)

  lazy val actionFactory = new ActivityUIActionFactory(this)
}
