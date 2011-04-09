package com.github.scala_android.crud

import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.ShouldMatchers
import android.os.ParcelFileDescriptor
import android.app.backup.{BackupDataInput, BackupDataOutput}

/**
 * A test for {@link CrudBackupAgent}.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/7/11
 * Time: 6:22 PM
 */
@RunWith(classOf[RobolectricTestRunner])
class CrudBackupAgentSpec extends MyEntityTesting with ShouldMatchers {
  val crudContext = new CrudContext(null)

  @Test
  def calculatedIteratorShouldWork() {
    val values = List("a", "b", "c").toIterator
    val iterator = new CalculatedIterator[String] {
      def calculateNextValue() = if (values.hasNext) Some(values.next()) else None
    }
    iterator.next() should be ("a")
    iterator.hasNext should be (true)
    iterator.hasNext should be (true)
    iterator.next() should be ("b")
    iterator.hasNext should be (true)
    iterator.next() should be ("c")
    iterator.hasNext should be (false)
    iterator.hasNext should be (false)
  }

  @Test
  def shouldSupportBackupAndRestore() {
    val application = mock[CrudApplication]
    val entityType = mock[CrudEntityType[_,_,_,_]]
    val dataOut = mock[BackupDataOutput]
    val dataIn = mock[BackupDataInput]
    val state0 = null
    val state1 = mock[ParcelFileDescriptor]
    val state1b = mock[ParcelFileDescriptor]
    expecting {
      call(application.allEntities).andReturn(List[CrudEntityTypeRef](entityType))
    }
    whenExecuting(entityType, dataOut, application, state1, state1b) {
      val backupAgent = new CrudBackupAgent(application)
      backupAgent.onCreate()
      backupAgent.onBackup(state0, dataOut, state1)
      backupAgent.onDestroy()

      val backupAgent2 = new CrudBackupAgent(application)
      backupAgent2.onCreate()
      backupAgent2.onRestore(dataIn, 1, state1b)
      backupAgent2.onDestroy()
    }
  }
}