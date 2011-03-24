package com.github.scala_android_crud.sample

import com.github.scala_android.crud.{CrudBackupAgent, CrudApplication}

/**
 * The sample application
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 4:53 PM
 */

object SampleApplication extends CrudApplication {
  val name = "Sample Application"

  def allEntities = List(AuthorCrudType, BookCrudType)
}

class SampleBackupAgent extends CrudBackupAgent(SampleApplication)