package com.github.scala_android.crud.generate

import android.view.View
import com.github.scala_android.crud.view.ViewField.ViewIdField
import java.lang.reflect.{Modifier, Field}
import java.lang.IllegalStateException
import com.github.scala_android.crud.common.PlatformTypes
import com.github.scala_android.crud.persistence.{IdPk, CursorField}
import com.github.scala_android.crud.{ForeignKey, CrudType}
import com.github.triangle._
import xml.Elem
import util.Random

/**
 * A UI Generator for a CrudTypes.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/4/11
 * Time: 3:19 PM
 */

object CrudUIGenerator extends PlatformTypes with Logging {
  private[generate] val random = new Random

  def generateLayouts(crudType: CrudType) {//}, baseOutputDirectory: Path = Directory.Current.get) {
    generateLayouts(crudType, detectResourceIdClasses(crudType.getClass))
  }

  private[generate] def detectResourceIdClasses(clazz: Class[_]): Seq[Class[_]] = {
    findResourceIdClass(clazz.getClassLoader, clazz.getPackage.getName).toSeq ++ Seq(classOf[android.R.id], classOf[com.github.scala_android.crud.res.R.id])
  }

  private[generate] def findResourceIdClass(classLoader: ClassLoader, packageName: String): Option[Class[_]] = {
    try { Some(classLoader.loadClass(packageName + ".R$id")) }
    catch { case e: ClassNotFoundException =>
      val parentPackagePieces = packageName.split('.').dropRight(1)
      if (parentPackagePieces.isEmpty) None else findResourceIdClass(classLoader, parentPackagePieces.mkString("."))
    }
  }

  protected def fieldLayoutForHeader(field: ViewFieldInfo, position: Int): Elem = {
    val textAppearance = if (position < 2) "?android:attr/textAppearanceLarge" else "?android:attr/textAppearanceSmall"
    val gravity = if (position % 1 == 0) "left" else "right"
    val layoutWidth = if (position % 1 == 0) "wrap_content" else "fill_parent"
    <TextView android:text={field.displayName} android:gravity={gravity}
              android:layout_width={layoutWidth}
              android:layout_height="wrap_content"
              android:paddingRight="3sp"
              android:textAppearance={textAppearance}/>
  }

  protected def fieldLayoutForRow(field: ViewFieldInfo, position: Int): Elem = {
    val textAppearance = if (position < 2) "?android:attr/textAppearanceLarge" else "?android:attr/textAppearanceSmall"
    val gravity = if (position % 1 == 0) "left" else "right"
    val layoutWidth = if (position % 1 == 0) "wrap_content" else "fill_parent"
    val attributes = <TextView android:id={"@+id/" + field.id} android:gravity={gravity}
                               android:layout_width={layoutWidth}
                               android:layout_height="wrap_content"
                               android:paddingRight="3sp"
                               android:textAppearance={textAppearance}/>.attributes
    field.layout.displayXml % attributes
  }

  protected def headerLayout(fields: List[ViewFieldInfo]) =
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                  android:paddingTop="2dip"
                  android:paddingBottom="2dip"
                  android:paddingLeft="6dip"
                  android:paddingRight="6dip"
                  android:layout_width="match_parent"
                  android:layout_height="wrap_content"
                  android:minHeight="?android:attr/listPreferredItemHeight"
                  android:orientation="vertical">{
      fields.sliding(2).map { rowFields =>
        <LinearLayout android:layout_width="match_parent"
                      android:layout_height="wrap_content"
                      android:orientation="horizontal">
          {rowFields.map(field => fieldLayoutForHeader(field, fields.indexOf(field)))}
        </LinearLayout>
      }
    }
    </LinearLayout>

  protected def rowLayout(fields: List[ViewFieldInfo]) =
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                  android:paddingTop="2dip"
                  android:paddingBottom="2dip"
                  android:paddingLeft="6dip"
                  android:paddingRight="6dip"
                  android:layout_width="match_parent"
                  android:layout_height="wrap_content"
                  android:minHeight="?android:attr/listPreferredItemHeight"
                  android:orientation="vertical">{
      fields.sliding(2).map { rowFields =>
        <LinearLayout android:layout_width="match_parent"
                      android:layout_height="wrap_content"
                      android:orientation="horizontal">
          {rowFields.map(field => fieldLayoutForRow(field, fields.indexOf(field)))}
        </LinearLayout>
      }
    }
    </LinearLayout>

  protected def fieldLayoutForEntry(field: ViewFieldInfo, position: Int): Elem = {
    val gravity = "right"
    val textAppearance = "?android:attr/textAppearanceLarge"
    val attributes = <EditText android:id="@+id/name"/>.attributes
    <TableRow>
      <TextView android:text={field.displayName + ":"} android:textAppearance={textAppearance} android:gravity={gravity}/>
      {field.layout.editXml % attributes}
    </TableRow>
  }

  def entryLayout(fields: List[ViewFieldInfo]) =
    <TableLayout xmlns:android="http://schemas.android.com/apk/res/android"
                 android:layout_width="fill_parent"
                 android:layout_height="wrap_content"
                 android:stretchColumns="1">
      {fields.map(field => fieldLayoutForEntry(field, fields.indexOf(field)))}
    </TableLayout>

  def guessFieldInfo(field: BaseField, resourceIdClasses: Seq[Class[_]]): ViewFieldInfo = {
    val viewIdFields = this.viewIdFields(field)
    val viewFieldsWithId = this.viewFields(FieldList.toFieldList(viewIdFields))
    val otherViewFields = this.viewFields(field).filterNot(viewFieldsWithId.contains)
    val viewResourceIds = viewIdFields.map(_.viewResourceId).map { id =>
      findFieldWithIntValue(resourceIdClasses, id).map(_.getName).getOrElse {
        throw new IllegalStateException("Unable to find R.id with value " + id)
      }
    }
    val foreignKeys = ForeignKey.foreignKeys(field)
    val persistedFieldsInForeignKeys = foreignKeys.flatMap(CursorField.persistedFields(_))
    val otherPersistedFields = CursorField.persistedFields(field).filterNot(persistedFieldsInForeignKeys.contains)
    val persistedFieldsWithTypes = otherPersistedFields.map(p => p.toString + ":" + p.persistedType.valueManifest.erasure.getSimpleName)
    println("viewIds: " + viewResourceIds + " tied to " +
            viewFieldsWithId + "  /  other views: " + otherViewFields + "  /  foreignKeys: " + foreignKeys +
            " / other persisted: " + persistedFieldsWithTypes)
    val persistedFieldOption = otherPersistedFields.headOption
    val displayName = persistedFieldOption.map(_.name).getOrElse("field" + random.nextInt())
    val fieldLayout = field.deepCollect {
      case _: PortableField[Double] => FieldLayout.doubleLayout
      case _: PortableField[String] => FieldLayout.nameLayout
      case _: PortableField[Int] => FieldLayout.intLayout
    }.head
    ViewFieldInfo(displayName, fieldLayout, FieldLayout.toId(displayName))
  }

  def guessFieldInfos(crudType: CrudType, resourceIdClasses: Seq[Class[_]]): List[ViewFieldInfo] = {
    val excludedFields = List(CursorField.persistedId, IdPk.idField)
    crudType.fields.filterNot(excludedFields.contains).map(guessFieldInfo(_, resourceIdClasses))
  }

  def generateLayouts(crudType: CrudType, resourceIdClasses: Seq[Class[_]]) {
    println("Generating layout for " + crudType)
    val fieldInfos = guessFieldInfos(crudType, resourceIdClasses)
    println(rowLayout(fieldInfos))
    println(entryLayout(fieldInfos))
  }

  private def findFieldWithIntValue(classes: Seq[Class[_]], value: Int): Option[Field] = {
    classes.view.flatMap(_.getDeclaredFields.find { field =>
      Modifier.isStatic(field.getModifiers) && field.getInt(null) == value
    }).headOption
  }

  private[generate] def viewFields(field: BaseField): List[SubjectField] = {
    field.deepCollect[SubjectField] {
      case subjectField: SubjectField if classOf[View].isAssignableFrom(subjectField.subjectManifest.erasure) => {
        subjectField
      }
    }
  }

  private[generate] def viewIdFields(field: BaseField): List[ViewIdField[_]] = {
    field.deepCollect[ViewIdField[_]] {
      case viewIdField: ViewIdField[_] => viewIdField
    }
  }
}

/**
 * The layout piece for a field.
 * It provides the XML for the part of an Android Layout that corresponds to a single field.
 * Standards attributes are separately added such as android:id and those needed by the parent View.
 */
abstract class FieldLayout {
  def displayXml: Elem
  def editXml: Elem
}

object FieldLayout {
  def textLayout(inputType: String) = new FieldLayout {
    def displayXml = <TextView/>
    def editXml = <EditText android:inputType={inputType}/>
  }

  lazy val nameLayout = textLayout("textCapWords")
  lazy val intLayout = textLayout("number|numberSigned")
  lazy val doubleLayout = textLayout("numberDecimal|numberSigned")
  lazy val currencyLayout = textLayout("numberDecimal|numberSigned")

  private[generate] def toId(displayName: String): String = {
    val id = displayName.collect {
      case c if Character.isJavaIdentifierPart(c) => c
    }.dropWhile(!Character.isJavaIdentifierStart(_))
    id.head.toLower + id.tail
  }
}

case class ViewFieldInfo(displayName: String, layout: FieldLayout, id: String)
