package geeks.crud.android

import android.os.Bundle
import android.app.{AlertDialog, ListActivity}
import android.widget.{SimpleCursorAdapter, ListAdapter, CursorAdapter}
import geeks.crud.EntityPersistenceComponent
import android.net.Uri
import android.database.Cursor
import android.view.{View, MenuItem, Menu}
import android.content.{ContentValues, Context, DialogInterface}

/**
 * A generic ListActivity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 */
trait CrudListActivity extends ListActivity with EntityPersistenceComponent {
  protected val ADD_DIALOG_ID = 100
  protected val EDIT_DIALOG_ID = 101

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

  def fields: List[Field]

  def listAdapter: ListAdapter

  def context: Context = this

  def refreshAfterSave()

  lazy val contentProviderAuthority = this.getClass.getPackage.toString
  lazy val defaultContentUri = Uri.parse("content://" + contentProviderAuthority + "/" + entityName);

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

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
    entityId.map(persistence.find).map(cursor => fields.foreach(_.readIntoView(cursor, entryView)))
    builder.setView(entryView)
    builder.setTitle(if (entityId.isDefined) editDialogTitleString else addDialogTitleString)
    builder.setPositiveButton(if (entityId.isDefined) editItemString else addItemString, new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        dialog.dismiss
        val contentValues = new ContentValues()
        fields.foreach(_.writeFromView(entryView, contentValues))
        persistence.save(entityId, contentValues)
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
                                                     //todo combine into CrudListActivity
trait CursorCrudListActivity extends CrudListActivity {
  lazy val viewResourceIds: List[Int] = fields.flatMap(_ match {
    case viewField: ViewField => List(viewField.viewResourceId)
    case _ => Nil
  })

  lazy val dataSource: CursorAdapter = {
    val cursor = persistence.findAll
    val adapter = new SimpleCursorAdapter(this, rowLayout, cursor,
      //provide the field names but making sure that they have the same length as the viewResourceIds.
      //These aren't actually used by the ViewBinder below.
      (cursor.getColumnNames.toList ::: cursor.getColumnNames.toList).slice(0, viewResourceIds.size).toArray,
      viewResourceIds.toArray)
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      def setViewValue(view: View, cursor: Cursor, columnIndex: Int) = {
        fields.foreach(field => field match {
          case viewField: ViewField => {
            viewField.readIntoFieldView(cursor, view)
            List(viewField)
          }
          case _ => Nil
        })
        true
      }
    })
    adapter
  }

  def listAdapter: ListAdapter = dataSource

  override def refreshAfterSave() = dataSource.getCursor.requery
}
