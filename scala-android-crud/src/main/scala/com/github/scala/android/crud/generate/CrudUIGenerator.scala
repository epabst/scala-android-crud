package com.github.scala.android.crud.generate

import android.view.View
import java.lang.IllegalStateException
import com.github.scala.android.crud.common.PlatformTypes
import com.github.scala.android.crud.persistence.{CursorField}
import com.github.triangle._
import util.Random
import scala.tools.nsc.io.Path
import xml._
import com.github.scala.android.crud.view.{ViewField, FieldLayout,AndroidResourceAnalyzer}
import AndroidResourceAnalyzer._
import com.github.scala.android.crud.view.ViewField.{ViewIdNameField, ViewIdField}
import com.github.scala.android.crud.{NamingConventions, CrudApplication, ParentField, CrudType}

/**
 * A UI Generator for a CrudTypes.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 8/4/11
 * Time: 3:19 PM
 */

object CrudUIGenerator extends PlatformTypes with Logging {
  protected lazy val logTag = getClass.getSimpleName
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

  private def writeXmlToFile(path: Path, xml: Node) {
    val file = path.toFile
    file.parent.createDirectory()
    file.writeAll("""<?xml version="1.0" encoding="utf-8"?>""", lineSeparator, prettyPrinter.format(xml))
    println("Wrote " + file)
  }

  def generateAndroidManifest(application: CrudApplication): Node = {
    val activityNames = application.allEntities.flatMap { entity =>
      List(entity.listActivityClass.getSimpleName, entity.activityClass.getSimpleName)
    }
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
              package={application.packageName}
              android:versionName="${project.version}"
              android:versionCode="${versionCode}">
      <application android:label="@string/app_name" android:icon="@drawable/icon"
                   android:theme="@android:style/Theme.NoTitleBar"
                   android:debuggable="${android.debuggable}"
                   android:backupAgent={application.classNamePrefix + "BackupAgent"} android:restoreAnyVersion="true">
        <meta-data android:name="com.google.android.backup.api_key"
                   android:value="TODO: get a backup key from http://code.google.com/android/backup/signup.html and put it here."/>
        <activity android:name={"." + activityNames.head} android:label="@string/app_name">
          <intent-filter>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
          </intent-filter>
        </activity>
        {activityNames.tail.map { name => <activity android:name={"." + name} android:label="@string/app_name"/>}}
      </application>
      <uses-sdk android:minSdkVersion="8"/>
    </manifest>
  }

  def generateValueStrings(entity: CrudType): NodeSeq = {
    <string name={entity.entityNameLayoutPrefix + "_list"}>{entity.entityName} List</string> +: {
      if (entity.createAction.isDefined) {
        Seq(<string name={"add_" + entity.entityNameLayoutPrefix}>Add {entity.entityName}</string>,
            <string name={"edit_" + entity.entityNameLayoutPrefix}>Edit {entity.entityName}</string>)
      } else {
        Nil
      }
    }
  }

  def generateValueStrings(application: CrudApplication): Node = {
    <resources>
      <string name="app_name">{application.name}</string>
      {application.allEntities.flatMap(generateValueStrings(_))}
    </resources>
  }

  def generateLayouts(application: CrudApplication) {
    application.allEntities.foreach(generateLayouts(_))
    writeXmlToFile(Path("AndroidManifest.xml"), generateAndroidManifest(application))
    writeXmlToFile(Path("res") / "values" / "strings.xml", generateValueStrings(application))
  }

  protected[generate] def fieldLayoutForHeader(field: ViewFieldInfo, position: Int): Elem = {
    val textAppearance = if (position < 2) "?android:attr/textAppearanceLarge" else "?android:attr/textAppearanceSmall"
    val gravity = if (position % 2 == 0) "left" else "right"
    val layoutWidth = if (position % 2 == 0) "wrap_content" else "fill_parent"
    <TextView android:text={field.displayName.getOrElse("")} android:gravity={gravity}
              android:layout_width={layoutWidth}
              android:layout_height="wrap_content"
              android:paddingRight="3sp"
              android:textAppearance={textAppearance}/>
  }

  protected[generate] def fieldLayoutForRow(field: ViewFieldInfo, position: Int): Elem = {
    val textAppearance = if (position < 2) "?android:attr/textAppearanceLarge" else "?android:attr/textAppearanceSmall"
    val gravity = if (position % 2 == 0) "left" else "right"
    val layoutWidth = if (position % 2 == 0) "wrap_content" else "fill_parent"
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

  def guessFieldInfo(field: BaseField, rIdClasses: Seq[Class[_]]): ViewFieldInfo = {
    val updateablePersistedFields = CursorField.updateablePersistedFields(field, rIdClasses)
    val persistedFieldOption = updateablePersistedFields.headOption

    val viewIdFields = this.viewIdFields(field)
    val viewIdNameFields = this.viewIdNameFields(field)
    val viewFieldsWithId = this.fieldsWithViewSubject(FieldList.toFieldList(viewIdFields))
    val otherViewFields = this.fieldsWithViewSubject(field).filterNot(viewFieldsWithId.contains)
    val viewResourceIdNames = viewIdNameFields.map(_.viewResourceIdName) ++ viewIdFields.map(_.viewResourceId).map { id =>
      findResourceFieldWithIntValue(rIdClasses, id).map(_.getName).getOrElse {
        throw new IllegalStateException("Unable to find R.id with value " + id)
      }
    }
    val persistedFieldsWithTypes = updateablePersistedFields.map(p => p.toString + ":" + p.persistedType.valueManifest.erasure.getSimpleName)
    println("viewIds: " + viewResourceIdNames + " tied to " +
            viewFieldsWithId + "  /  other views: " + otherViewFields +
            "  /  parentFields: " + ParentField.parentFields(field) +
            " / other persisted: " + persistedFieldsWithTypes)
    val derivedId: Option[String] = viewResourceIdNames.headOption.orElse(persistedFieldOption.map(_.name))
    val displayName = derivedId.map(FieldLayout.toDisplayName(_))
    val fieldLayout = viewFields(field).headOption.map(_.defaultLayout).getOrElse(field.deepCollect {
      case _: PortableField[Double] => FieldLayout.doubleLayout
      case _: PortableField[String] => FieldLayout.nameLayout
      case _: PortableField[Int] => FieldLayout.intLayout
    }.head)
    val displayable = !viewResourceIdNames.isEmpty
    ViewFieldInfo(displayName, fieldLayout, id = derivedId.getOrElse("field" + random.nextInt()), displayable,
      updateable = displayable && persistedFieldOption.isDefined)
  }

  def guessFieldInfos(crudType: CrudType): List[ViewFieldInfo] =
    crudType.fields.map(guessFieldInfo(_, crudType.rIdClasses))

  private def writeLayoutFile(name: String, xml: Elem) {
    writeXmlToFile(Path("res") / "layout" / (name + ".xml"), xml)
  }

  def generateLayouts(crudType: CrudType) {
    println("Generating layout for " + crudType)
    val fieldInfos = guessFieldInfos(crudType)
    val displayFields = fieldInfos.filter(_.displayable)
    val updateableFields = fieldInfos.filter(_.updateable)
    val layoutPrefix = NamingConventions.toLayoutPrefix(crudType.entityName)
    writeLayoutFile(layoutPrefix + "_header", headerLayout(displayFields))
    writeLayoutFile(layoutPrefix + "_row", rowLayout(displayFields))
    if (!updateableFields.isEmpty) writeLayoutFile(layoutPrefix + "_entry", entryLayout(updateableFields))
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
