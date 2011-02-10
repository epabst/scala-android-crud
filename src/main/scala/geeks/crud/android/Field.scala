package geeks.crud.android

import android.view.View
import android.widget.EditText
import geeks.crud.{BasicValueFormat, ValueFormat}

/**
 * A Field that may be editable in a View and/or persisted.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/4/11
 * Time: 9:25 PM
 */
trait Field[E] {
  def copyToView(fromEntity: E, toEntryView: View)
  def copyToEntity(fromEntryView: View, toEntity: E)

  //These should correspond with parallel entries in persistedFieldNamesWithView
  val viewResourceIds: List[Int]
  val persistedFieldNamesWithView: List[String]

  /** The fields (other than those assumed by the EntityPersistence such as the ID) */
  val queryFieldNames: List[String]
  /** These will be put into a {@link android.content.ContentValues} */
  def valuesToPersist(entity: E): Map[String, Any]
}

trait UnpersistedField[E] extends Field[E] {
  val queryFieldNames = Nil
  val persistedFieldNamesWithView = Nil

  def copyToEntity(fromEntryView: View, toEntity: E) {}
  def valuesToPersist(entity: E): Map[String, Any] = Map.empty
}

trait ViewField[E,V] extends Field[E] {
  protected def format: ValueFormat[V]

  val resourceId: Int

  val viewResourceIds = List(resourceId)

  private def fieldView(entryView: View): View = entryView.findViewById(resourceId)

  def setValueFromView(entity: E, valueFromView: V)

  def getValueForView(entity: E): V

  def copyToEntity(fromEntryView: View, toEntity: E) = setValueFromView(toEntity, getValue(fieldView(fromEntryView)))

  def copyToView(fromEntity: E, toEntryView: View) = setValue(fieldView(toEntryView), getValueForView(fromEntity))

  def getValue(fieldView: View): V = fieldView match {
    //add more cases as needed
    case v: EditText => format.toValue(v.getText.toString)
    case v => throw new IllegalStateException("Unrecognized view: " + v)
  }

  def setValue(fieldView: View, value: V) = fieldView match {
    //add more cases as needed
    case v: EditText => v.setText(format.toString(value))
    case v => throw new IllegalStateException("Unrecognized view: " + v)
  }
}

trait PersistedField[E,P] extends Field[E] {
  val persistedName: String
  val queryFieldNames = List(persistedName)
  //only include it if viewResourceIds is not empty
  val persistedFieldNamesWithView: List[String] = viewResourceIds.headOption.map(_ => persistedName).toList

  def getPersistedValue(entity: E): P

  def valuesToPersist(entity: E): Map[String,Any] = Map.empty + (persistedName -> getPersistedValue(entity))
}

class SimpleField[E,V](val persistedName: String, val resourceId: Int, getter: E => V, setter: E => V => Unit)(implicit m: Manifest[V])
        extends ViewField[E,V] with PersistedField[E,V] {

  lazy val format = new BasicValueFormat[V]

  final def getPersistedValue(entity: E) = getter(entity)

  final def getValueForView(entity: E) = getter(entity)

  final def setValueFromView(entity: E, valueFromView: V) = setter(entity)(valueFromView)
}
