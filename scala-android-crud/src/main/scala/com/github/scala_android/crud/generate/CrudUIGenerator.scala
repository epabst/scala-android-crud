package com.github.scala_android.crud.generate

import android.view.View
import java.lang.IllegalStateException
import com.github.scala_android.crud.common.PlatformTypes
import com.github.scala_android.crud.persistence.{IdPk, CursorField}
import com.github.triangle._
import util.Random
import scala.tools.nsc.io.Path
import xml._
import com.github.scala_android.crud.{CrudApplication, ParentField, CrudType}
import com.github.scala_android.crud.view.{ViewField, FieldLayout,AndroidResourceAnalyzer}
import AndroidResourceAnalyzer._
import com.github.scala_android.crud.view.ViewField.{ViewIdNameField, ViewIdField}

/**
 * A UI Generator for a CrudTypes.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/4/11
 * Time: 3:19 PM
 */

object CrudUIGenerator extends PlatformTypes with Logging {
  private val lineSeparator = System.getProperty("line.separator")
  private[generate] val random = new Random
  private[generate] val prettyPrinter = new PrettyPrinter(80, 2) {
    override protected def traverse(node: Node, namespace: NamespaceBinding, indent: Int) {
      node match {
        case Text(text) if text.trim().size == 0 => super.traverse(Text(text.trim()), namespace, indent)
        case n: Elem if n.child.size == 0 => makeBox(indent, leafTag(n))
        case _ => super.traverse(node, namespace, indent)
      }
    }
  }

  def generateLayouts(crudType: CrudType) {
    generateLayouts(crudType, detectResourceIdClasses(crudType.getClass))
  }

  def generateLayouts(application: CrudApplication) {
    application.allEntities.foreach(generateLayouts(_))
  }

  protected def fieldLayoutForHeader(field: ViewFieldInfo, position: Int): Elem = {
    val textAppearance = if (position < 2) "?android:attr/textAppearanceLarge" else "?android:attr/textAppearanceSmall"
    val gravity = if (position % 1 == 0) "left" else "right"
    val layoutWidth = if (position % 1 == 0) "wrap_content" else "fill_parent"
    <TextView android:text={field.displayName.getOrElse("")} android:gravity={gravity}
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
      fields.grouped(2).map { rowFields =>
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
      fields.grouped(2).map { rowFields =>
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
    val attributes = <EditText android:id={"@+id/" + field.id}/>.attributes
    <TableRow>
      <TextView android:text={field.displayName.get + ":"} android:textAppearance={textAppearance} android:gravity={gravity}/>
      {field.layout.editXml % attributes}
    </TableRow>
  }

  def entryLayout(fields: List[ViewFieldInfo]) = {
    <TableLayout xmlns:android="http://schemas.android.com/apk/res/android"
                 android:layout_width="fill_parent"
                 android:layout_height="wrap_content"
                 android:stretchColumns="1">
      {fields.map(field => fieldLayoutForEntry(field, fields.indexOf(field)))}
    </TableLayout>
  }

  def guessFieldInfo(field: BaseField, resourceIdClasses: Seq[Class[_]]): ViewFieldInfo = {
    val viewIdFields = this.viewIdFields(field)
    val viewIdNameFields = this.viewIdNameFields(field)
    val viewFieldsWithId = this.fieldsWithViewSubject(FieldList.toFieldList(viewIdFields))
    val otherViewFields = this.fieldsWithViewSubject(field).filterNot(viewFieldsWithId.contains)
    val viewResourceIdNames = viewIdNameFields.map(_.viewResourceIdName) ++ viewIdFields.map(_.viewResourceId).map { id =>
      findResourceFieldWithIntValue(resourceIdClasses, id).map(_.getName).getOrElse {
        throw new IllegalStateException("Unable to find R.id with value " + id)
      }
    }
    val parentFields = ParentField.parentFields(field)
    val parentFieldNames = parentFields.map(_.fieldName)
    val otherPersistedFields = CursorField.persistedFields(field).filterNot(parentFieldNames.contains(_))
    val persistedFieldsWithTypes = otherPersistedFields.map(p => p.toString + ":" + p.persistedType.valueManifest.erasure.getSimpleName)
    println("viewIds: " + viewResourceIdNames + " tied to " +
            viewFieldsWithId + "  /  other views: " + otherViewFields + "  /  parentFields: " + parentFields +
            " / other persisted: " + persistedFieldsWithTypes)
    val persistedFieldOption = otherPersistedFields.headOption
    val derivedId: Option[String] = viewResourceIdNames.headOption.orElse(persistedFieldOption.map(_.name))
    val displayName = derivedId.map(FieldLayout.toDisplayName(_))
    val fieldLayout = viewFields(field).headOption.map(_.defaultLayout).getOrElse(field.deepCollect {
      case _: PortableField[Double] => FieldLayout.doubleLayout
      case _: PortableField[String] => FieldLayout.nameLayout
      case _: PortableField[Int] => FieldLayout.intLayout
    }.head)
    ViewFieldInfo(displayName, fieldLayout, id = derivedId.getOrElse("field" + random.nextInt()),
      displayable = !viewResourceIdNames.isEmpty, updateable = persistedFieldOption.isDefined)
  }

  def guessFieldInfos(crudType: CrudType, resourceIdClasses: Seq[Class[_]]): List[ViewFieldInfo] = {
    val excludedFields = List(CursorField.persistedId, IdPk.idField)
    crudType.fields.filterNot(excludedFields.contains).map(guessFieldInfo(_, resourceIdClasses))
  }

  private def writeLayoutFile(name: String, xml: Elem) {
    val file = (Path("res") / "layout" / (name + ".xml")).toFile
    file.parent.createDirectory()
    file.writeAll("""<?xml version="1.0" encoding="utf-8"?>""", lineSeparator, prettyPrinter.format(xml))
    println("Wrote " + file)
  }

  def toFilename(string: String): String = string.collect {
    case c if (c.isUpper) => "_" + c.toLower
    case c if (Character.isJavaIdentifierPart(c)) => c.toString
  }.mkString.stripPrefix("_")

  def generateLayouts(crudType: CrudType, resourceIdClasses: Seq[Class[_]]) {
    println("Generating layout for " + crudType)
    val filenamePrefix = toFilename(crudType.entityName)
    val fieldInfos = guessFieldInfos(crudType, resourceIdClasses)
    val displayFields = fieldInfos.filter(_.displayable)
    val updateableFields = fieldInfos.filter(_.updateable)
    writeLayoutFile(filenamePrefix + "_header", headerLayout(displayFields))
    writeLayoutFile(filenamePrefix + "_row", rowLayout(displayFields))
    if (!updateableFields.isEmpty) writeLayoutFile(filenamePrefix + "_entry", entryLayout(updateableFields))
  }

  private[generate] def fieldsWithViewSubject(field: BaseField): List[SubjectField] = {
    field.deepCollect[SubjectField] {
      case matchingField: SubjectField if classOf[View].isAssignableFrom(matchingField.subjectManifest.erasure) => {
        matchingField
      }
    }
  }

  private[generate] def viewFields(field: BaseField): List[ViewField[_]] =
    field.deepCollect {
      case matchingField: ViewField[_] => matchingField
    }

  private[generate] def viewIdFields(field: BaseField): List[ViewIdField[_]] =
    field.deepCollect[ViewIdField[_]] {
      case matchingField: ViewIdField[_] => matchingField
    }

  private[generate] def viewIdNameFields(field: BaseField): List[ViewIdNameField[_]] =
    field.deepCollect[ViewIdNameField[_]] {
      case matchingField: ViewIdNameField[_] => matchingField
    }
}

case class ViewFieldInfo(displayName: Option[String], layout: FieldLayout, id: String,
                         displayable: Boolean, updateable: Boolean)
