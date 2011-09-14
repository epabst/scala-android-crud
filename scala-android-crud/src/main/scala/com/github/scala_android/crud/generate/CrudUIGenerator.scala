package com.github.scala_android.crud.generate

import android.view.View
import com.github.scala_android.crud.view.ViewField.ViewIdField
import java.lang.reflect.{Modifier, Field}
import java.lang.IllegalStateException
import com.github.scala_android.crud.common.PlatformTypes
import com.github.scala_android.crud.persistence.{IdPk, CursorField}
import com.github.triangle._
import util.Random
import scala.tools.nsc.io.Path
import xml._
import com.github.scala_android.crud.{CrudApplication, ForeignKey, CrudType}
import com.github.scala_android.crud.view.{ViewField, FieldLayout}

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
    val viewFieldsWithId = this.fieldsWithViewSubject(FieldList.toFieldList(viewIdFields))
    val otherViewFields = this.fieldsWithViewSubject(field).filterNot(viewFieldsWithId.contains)
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
    val derivedId: Option[String] = viewResourceIds.headOption.orElse(persistedFieldOption.map(_.name))
    val displayName = derivedId.map(FieldLayout.toDisplayName(_))
    val fieldLayout = viewFields(field).headOption.map(_.defaultLayout).getOrElse(field.deepCollect {
      case _: PortableField[Double] => FieldLayout.doubleLayout
      case _: PortableField[String] => FieldLayout.nameLayout
      case _: PortableField[Int] => FieldLayout.intLayout
    }.head)
    ViewFieldInfo(displayName, fieldLayout, persistedFieldOption.isDefined, foreignKeys.headOption,
      derivedId.getOrElse("field" + random.nextInt()))
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
    case c if (Character.isJavaLetterOrDigit(c)) => c.toString
  }.mkString.stripPrefix("_")

  def generateLayouts(crudType: CrudType, resourceIdClasses: Seq[Class[_]]) {
    println("Generating layout for " + crudType)
    val fieldInfos = guessFieldInfos(crudType, resourceIdClasses)
    val filenamePrefix = toFilename(crudType.entityName)
    val displayFields = fieldInfos.filterNot(_.foreignKey.isDefined)
    writeLayoutFile(filenamePrefix + "_header", headerLayout(displayFields))
    writeLayoutFile(filenamePrefix + "_row", rowLayout(displayFields))
    val updateableFields = fieldInfos.filter(_.updateable)
    if (!updateableFields.isEmpty) writeLayoutFile(filenamePrefix + "_entry", entryLayout(updateableFields))
  }

  private def findFieldWithIntValue(classes: Seq[Class[_]], value: Int): Option[Field] = {
    classes.view.flatMap(_.getDeclaredFields.find { field =>
      Modifier.isStatic(field.getModifiers) && field.getInt(null) == value
    }).headOption
  }

  private[generate] def fieldsWithViewSubject(field: BaseField): List[SubjectField] = {
    field.deepCollect[SubjectField] {
      case subjectField: SubjectField if classOf[View].isAssignableFrom(subjectField.subjectManifest.erasure) => {
        subjectField
      }
    }
  }

  private[generate] def viewFields(field: BaseField): List[ViewField[_]] =
    field.deepCollect {
      case field: ViewField[_] => field
    }

  private[generate] def viewIdFields(field: BaseField): List[ViewIdField[_]] = {
    field.deepCollect[ViewIdField[_]] {
      case viewIdField: ViewIdField[_] => viewIdField
    }
  }
}

case class ViewFieldInfo(displayName: Option[String], layout: FieldLayout,
                         updateable: Boolean, foreignKey: Option[ForeignKey], id: String)
