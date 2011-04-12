package com.github.scala_android.crud

import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.scalatest.matchers.ShouldMatchers
import android.os.ParcelFileDescriptor
import android.widget.ListAdapter
import android.provider.BaseColumns
import scala.collection.mutable
import org.easymock.{IAnswer, EasyMock}
import EasyMock.notNull

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
    val listAdapter = mock[ListAdapter]
    val backupTarget = mock[BackupTarget]
    val state1 = mock[ParcelFileDescriptor]
    val state1b = mock[ParcelFileDescriptor]

    val persistence = new MyEntityPersistence(List(
        mutable.Map(BaseColumns._ID -> 100L, "name" -> "Joe", "age" -> 30),
        mutable.Map(BaseColumns._ID -> 101L, "name" -> "Mary", "age" -> 28)))
    val persistence2 = new MyEntityPersistence(List(
        mutable.Map(BaseColumns._ID -> 101L, "city" -> "Los Angeles", "state" -> "CA"),
        mutable.Map(BaseColumns._ID -> 104L, "city" -> "Chicago", "state" -> "IL")))
    val entityType = new MyEntityType(persistence, listAdapter)
    val entityType2 = new MyEntityType(persistence2, listAdapter, "OtherMap")
    val state0 = null
    var restoreItems = mutable.ListBuffer[RestoreItem]()
    expecting {
      call(application.allEntities).andReturn(List[CrudEntityTypeRef](entityType, entityType2))
      backupTarget.writeEntity(eql("MyMap#100"), notNull()).andAnswer(saveRestoreItem(restoreItems))
      backupTarget.writeEntity(eql("MyMap#101"), notNull()).andAnswer(saveRestoreItem(restoreItems))
      backupTarget.writeEntity(eql("OtherMap#101"), notNull()).andAnswer(saveRestoreItem(restoreItems))
      backupTarget.writeEntity(eql("OtherMap#104"), notNull()).andAnswer(saveRestoreItem(restoreItems))
    }
    whenExecuting(application, listAdapter, backupTarget, state1, state1b) {
      val backupAgent = new CrudBackupAgent(application)
      backupAgent.onCreate()
      backupAgent.onBackup(state0, backupTarget, state1)
      backupAgent.onDestroy()

      val backupAgent2 = new CrudBackupAgent(application)
      backupAgent2.onCreate()
      backupAgent2.onRestore(restoreItems.toIterator, 1, state1b)
      backupAgent2.onDestroy()
    }
  }

  def saveRestoreItem(restoreItems: mutable.ListBuffer[RestoreItem]): IAnswer[Unit] = answer {
    val currentArguments = EasyMock.getCurrentArguments
    currentArguments(1).asInstanceOf[Option[Map[String,Any]]].foreach { map =>
      restoreItems += RestoreItem(currentArguments(0).asInstanceOf[String], map)
    }
  }
}