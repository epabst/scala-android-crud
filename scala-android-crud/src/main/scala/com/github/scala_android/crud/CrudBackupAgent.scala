package com.github.scala_android.crud

import android.os.ParcelFileDescriptor
import android.app.backup.{BackupDataOutput, BackupDataInput, BackupAgent}

/**
 * A BackupAgent for a CrudApplication.
 * It must be subclassed in order to put it into the AndroidManifest.xml.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/7/11
 * Time: 4:35 PM
 */

class CrudBackupAgent(application: CrudApplication) extends BackupAgent {
  final def onBackup(oldState: ParcelFileDescriptor, data: BackupDataOutput, newState: ParcelFileDescriptor) {
    onBackup(oldState, new BackupTarget {
      def writeEntity(key: String, dataArray: Option[Array[Byte]]) {
        data.writeEntityHeader(key, dataArray.map(_.length).getOrElse(-1))
        dataArray.map(array => data.writeEntityData(array, array.length))
      }
    }, newState)
  }

  def onBackup(oldState: ParcelFileDescriptor, data: BackupTarget, newState: ParcelFileDescriptor) {
    val entities = application.allEntities
  }

  final def onRestore(data: BackupDataInput, appVersionCode: Int, newState: ParcelFileDescriptor) {
    onRestore(new BackupSource with CalculatedIterator[(String, Array[Byte])] {
      def calculateNextValue(): Option[(String, Array[Byte])] = {
        if (data.readNextHeader) {
          val key = data.getKey
          val size = data.getDataSize
          val array = new Array[Byte](size)
          data.readEntityData(array, 0, size)
          Some((key, array))
        } else {
          None
        }
      }
    }, appVersionCode, newState)
  }

  def onRestore(data: BackupSource, appVersionCode: Int, newState: ParcelFileDescriptor) {
  }
}

private[crud] trait CalculatedIterator[T] extends Iterator[T] {
  private var calculatedNextValue: Option[Option[T]] = None

  def calculateNextValue(): Option[T]

  private def determineNextValue(): Option[T] = {
    if (!calculatedNextValue.isDefined) {
      calculatedNextValue = Some(calculateNextValue())
    }
    calculatedNextValue.get
  }

  def hasNext = determineNextValue().isDefined

  def next() = {
    val next = determineNextValue().get
    calculatedNextValue = None
    next
  }
}

trait BackupTarget {
  def writeEntity(key: String, dataArray: Option[Array[Byte]])
}

trait BackupSource extends Iterator[(String, Array[Byte])]