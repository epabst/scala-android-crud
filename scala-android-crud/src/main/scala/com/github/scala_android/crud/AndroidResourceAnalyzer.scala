package com.github.scala_android.crud

import java.lang.reflect.{Modifier, Field}

/**
 * An "R" analyzer.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/13/11
 * Time: 9:53 PM
 */

object AndroidResourceAnalyzer {
  def detectResourceIdClasses(clazz: Class[_]): Seq[Class[_]] = {
    findResourceIdClass(clazz.getClassLoader, clazz.getPackage.getName).toSeq ++ Seq(classOf[android.R.id], classOf[com.github.scala_android.crud.res.R.id])
  }

  private def findResourceIdClass(classLoader: ClassLoader, packageName: String): Option[Class[_]] = {
    try { Some(classLoader.loadClass(packageName + ".R$id")) }
    catch { case e: ClassNotFoundException =>
      val parentPackagePieces = packageName.split('.').dropRight(1)
      if (parentPackagePieces.isEmpty) None else findResourceIdClass(classLoader, parentPackagePieces.mkString("."))
    }
  }

  private def findMatchingResourceField(classes: Seq[Class[_]], matcher: Field => Boolean): Option[Field] = {
    classes.view.flatMap(_.getDeclaredFields.find { field =>
      Modifier.isStatic(field.getModifiers) && matcher(field)
    }).headOption
  }

  def findResourceFieldWithIntValue(classes: Seq[Class[_]], value: Int): Option[Field] =
    findMatchingResourceField(classes, field => field.getInt(null) == value)

  def findResourceFieldWithName(classes: Seq[Class[_]], name: String): Option[Field] =
    findMatchingResourceField(classes, field => field.getName == name)
}
