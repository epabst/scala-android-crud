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

  lazy val actionFactory = new UIActionFactory {
    def currentActivity = activity

    def startCreate(entityType: CrudEntityType) =
      throw new UnsupportedOperationException("not implemented yet")

    def displayList(entityType: CrudEntityType, criteriaSource: AnyRef) = new CrudUIAction(entityConfig, activity) {
      def apply() {
        //todo don't assume it's a SQLiteCrudListActivity
        startActivity(new Intent(null, Uri.withAppendedPath(getIntent.getData, entityConfig.entityName), activity, classOf[SQLiteCrudListActivity]))
      }
    }

    /** By default, just startUpdate instead of just displaying. */
    def display(entityType: CrudEntityType, id: ID) = startUpdate(entityConfig, id)

    def startUpdate(entityType: CrudEntityType, id: ID) =
      throw new UnsupportedOperationException("not implemented yet")

    def startDelete(entityType: CrudEntityType, ids: List[ID]) =
      throw new UnsupportedOperationException("not implemented yet")
  }
}