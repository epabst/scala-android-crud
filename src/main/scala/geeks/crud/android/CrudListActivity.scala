package geeks.crud.android

import android.os.Bundle
import android.app.{AlertDialog, ListActivity}
import android.widget.ListAdapter
import geeks.crud.EntityPersistenceComponent
import android.net.Uri
import android.view.{View, MenuItem, Menu}
import android.content.{Context, DialogInterface}

/**
 * A generic ListActivity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 * @param L the type of findAll (e.g. Cursor)
 * @param R the type to read from (e.g. Cursor)
 * @param W the type to write to (e.g. ContentValues)
 */
trait CrudListActivity[L,R <: AnyRef,W <: AnyRef] extends ListActivity with EntityPersistenceComponent[L,R,W] {
  val ADD_DIALOG_ID = 100
  val EDIT_DIALOG_ID = 101

  def entityName: String

  def listLayout: Int
  def headerLayout: Int
  def rowLayout: Int
  def entryLayout: Int
  def addItemString: Int
  def addDialogTitleString: Int
  def editItemString: Int
  def editDialogTitleString: Int
  def cancelItemString: Int

  def fields: List[CopyableField]

  //todo eliminate this if possible
  lazy val viewResourceIds: List[Int] = fields.flatMap(_ match {
    case viewField: ViewFieldAccessById[_,_] => List(viewField.viewResourceId)
    case _ => Nil
  })

  def listAdapter: ListAdapter

  def context: Context = this

  def refreshAfterSave()

  lazy val contentProviderAuthority = this.getClass.getPackage.toString
  lazy val defaultContentUri = Uri.parse("content://" + contentProviderAuthority + "/" + entityName);

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    setContentView(geeks.financial.futurebalance.android.R.layout.entity_list)

    // If no data was given in the intent (because we were started
    // as a MAIN activity), then use our default content provider.
    if (getIntent.getData() == null) getIntent.setData(defaultContentUri);

    val view = getListView();
		view.setHeaderDividersEnabled(true);
		view.addHeaderView(getLayoutInflater().inflate(headerLayout, null));

    setListAdapter(listAdapter)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    menu.add(0, ADD_DIALOG_ID, 1, addItemString)
    true
  }

  override def onMenuItemSelected(featureId: Int, item: MenuItem): Boolean = {
    if (item.getItemId == ADD_DIALOG_ID) {
      showDialog(ADD_DIALOG_ID)
    }
    true
  }

  /**
   * Creates an edit dialog in the given Context to edit the entity and save it.
   * @param entityToEdit an Entity instance to edit or None to add a new one
   */
  def createEditDialog(context: Context, entityId: Option[ID], afterSave: () => Unit): AlertDialog = {
    val builder = new AlertDialog.Builder(context)
    val entryView = getLayoutInflater.inflate(entryLayout, null)
    entityId.map(persistence.find).map(readable => fields.foreach(_.copy(readable, entryView)))
    builder.setView(entryView)
    builder.setTitle(if (entityId.isDefined) editDialogTitleString else addDialogTitleString)
    builder.setPositiveButton(if (entityId.isDefined) editItemString else addItemString, new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        dialog.dismiss
        val writable = persistence.newWritable
        fields.foreach(_.copy(entryView, writable))
        persistence.save(entityId, writable)
        afterSave()
      }
    })
    builder.setNegativeButton(cancelItemString, new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        dialog.cancel
      }
    })
    builder.create
  }

  protected override def onCreateDialog(id: Int) = {
    createEditDialog(this, None, refreshAfterSave)
  }
}
