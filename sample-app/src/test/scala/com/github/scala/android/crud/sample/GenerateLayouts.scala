package com.github.scala.android.crud.sample

import com.github.scala.android.crud.generate.CrudUIGenerator

/**
 * A layout generator for the application.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/6/11
 * Time: 11:17 PM
 */

object GenerateLayouts {
  def main(args: Array[String]) {
    CrudUIGenerator.generateLayouts(SampleApplication)
  }
}