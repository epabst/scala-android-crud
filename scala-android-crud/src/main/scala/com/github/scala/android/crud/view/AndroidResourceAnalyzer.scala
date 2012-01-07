package com.github.scala.android.crud.view

import java.lang.reflect.{Modifier, Field}
import com.github.triangle.Logging
import com.github.scala.android.crud.common.Common

/** An "R" analyzer.
  * @author Eric Pabst (epabst@gmail.com)
  */

object AndroidResourceAnalyzer extends Logging {
  protected def logTag = Common.logTag

  private def findRInnerClass(classInSamePackage: Class[_], innerClassName: String): Option[Class[_]] = {
    findRInnerClass(classInSamePackage.getClassLoader, classInSamePackage.getPackage.getName, innerClassName)
  }

  private def findRInnerClass(classLoader: ClassLoader, packageName: String, innerClassName: String): Option[Class[_]] = {
    try { Some(classLoader.loadClass(packageName + ".R$" + innerClassName)) }
    catch { case e: ClassNotFoundException =>
      val parentPackagePieces = packageName.split('.').dropRight(1)
      if (parentPackagePieces.isEmpty) None
      else findRInnerClass(classLoader, parentPackagePieces.mkString("."), innerClassName)
    }
  }

  private def findMatchingResourceField(classes: Seq[Class[_]], matcher: Field => Boolean): Option[Field] = {
    classes.view.flatMap(_.getDeclaredFields.find { field =>
      Modifier.isStatic(field.getModifiers) && matcher(field)
    }).headOption
  }

  def detectRIdClasses(clazz: Class[_]): Seq[Class[_]] = {
    findRInnerClass(clazz, "id").toSeq ++ Seq(classOf[android.R.id], classOf[com.github.scala.android.crud.res.R.id])
  }

  def detectRLayoutClasses(clazz: Class[_]): Seq[Class[_]] = {
    findRInnerClass(clazz, "layout").toSeq ++ Seq(classOf[android.R.layout], classOf[com.github.scala.android.crud.res.R.layout])
  }

  def detectRStringClasses(clazz: Class[_]): Seq[Class[_]] = {
    findRInnerClass(clazz, "string").toSeq ++ Seq(classOf[android.R.string], classOf[com.github.scala.android.crud.res.R.string])
  }

  def findResourceFieldWithIntValue(classes: Seq[Class[_]], value: Int): Option[Field] =
    findMatchingResourceField(classes, field => field.getInt(null) == value)

  def resourceFieldWithIntValue(classes: Seq[Class[_]], value: Int): Field =
    findResourceFieldWithIntValue(classes, value).getOrElse {
      classes.foreach(rStringClass => error("Contents of " + rStringClass + " are " + rStringClass.getFields.mkString(", ")))
      throw new IllegalStateException("Unable to find R.id with value " + value + " not found.  You may want to run the CrudUIGenerator.generateLayouts." +
              classes.mkString("(string classes: ", ",", ")"))
    }

  def findResourceFieldWithName(classes: Seq[Class[_]], name: String): Option[Field] =
    findMatchingResourceField(classes, field => field.getName == name)

  def findResourceIdWithName(classes: Seq[Class[_]], name: String): Option[Int] =
    findResourceFieldWithName(classes, name).map(_.getInt(null))

  def resourceIdWithName(classes: Seq[Class[_]], name: String): Int =
    findResourceIdWithName(classes, name).getOrElse {
      classes.foreach(rStringClass => error("Contents of " + rStringClass + " are " + rStringClass.getFields.mkString(", ")))
      throw new IllegalStateException("R.string." + name + " not found.  You may want to run the CrudUIGenerator.generateLayouts." +
              classes.mkString("(string classes: ", ",", ")"))
    }
}
