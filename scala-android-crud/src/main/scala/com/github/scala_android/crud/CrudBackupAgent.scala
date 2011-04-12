package com.github.scala_android.crud

import android.app.backup.{BackupDataOutput, BackupDataInput, BackupAgent}
import monitor.Logging
import scala.collection.mutable
import scala.collection.JavaConversions._
import android.os.{Parcel, ParcelFileDescriptor}
import CursorFieldAccess._

/**
 * A BackupAgent for a CrudApplication.
 * It must be subclassed in order to put it into the AndroidManifest.xml.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 4/7/11
 * Time: 4:35 PM
 */

class CrudBackupAgent(application: CrudApplication) extends BackupAgent with Logging {
  final def onBackup(oldState: ParcelFileDescriptor, data: BackupDataOutput, newState: ParcelFileDescriptor) {
    onBackup(oldState, new BackupTarget {
      def writeEntity(key: String, dataMap: Option[Map[String,Any]]) {
        debug("Backing up " + key + " <- " + dataMap)
        dataMap match {
          case Some(map) =>
            val parcel = Parcel.obtain()
            try {
              parcel.writeMap(map)
              val bytes = parcel.marshall()
              data.writeEntityHeader(key, bytes.length)
              data.writeEntityData(bytes, bytes.length)
            } finally parcel.recycle()
          case None => data.writeEntityHeader(key, -1)
        }
      }
    }, newState)
  }

  def onBackup(oldState: ParcelFileDescriptor, data: BackupTarget, newState: ParcelFileDescriptor) {
    info("Backing up " + application)
    val crudContext = new CrudContext(this)
    application.allEntities.map(_ match {
      case entityType: CrudEntityType[_,_,_,_] => onBackup(entityType, data, crudContext)
    })
  }

  def onBackup[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef](entityType: CrudEntityType[Q,L,R,W], data: BackupTarget, crudContext: CrudContext) {
    entityType.withEntityPersistence[Unit](crudContext, persistence => {
      val all = persistence.findAll(persistence.newCriteria)
      persistence.toIterator(all).foreach(entity => {
        val map = mutable.Map[String,Any]()
        entityType.copyFields(entity, map)
        val id = persistedId.partialGet(entity).get
        data.writeEntity(entityType.entityName + "#" + id, Some(map.toMap))
      })
    })
  }

  final def onRestore(data: BackupDataInput, appVersionCode: Int, newState: ParcelFileDescriptor) {
    onRestore(new CalculatedIterator[RestoreItem] {
      def calculateNextValue(): Option[RestoreItem] = {
        if (data.readNextHeader) {
          val map = Map[String,Any]()
          val key = data.getKey
          val size = data.getDataSize
          val bytes = new Array[Byte](size)
          data.readEntityData(bytes, 0, size)
          val parcel = Parcel.obtain
          try {
            parcel.unmarshall(bytes, 0, size)
            parcel.readMap(map, getClass.getClassLoader)
          } finally parcel.recycle()
          Some(RestoreItem(key, map.toMap))
        } else {
          None
        }
      }
    }, appVersionCode, newState)
  }

  def onRestore(data: Iterator[RestoreItem], appVersionCode: Int, newState: ParcelFileDescriptor) {
    info("Restoring backup of " + application)
    val crudContext = new CrudContext(this)
    val entities = application.allEntities
    data.foreach(restoreItem => {
      val entityName = restoreItem.key.substring(0, restoreItem.key.lastIndexOf("#"))
      entities.find(_.entityName == entityName).map(_ match {
        case entityType: CrudEntityType[_,_,_,_] => onRestore(entityType, restoreItem, crudContext)
      })
    })
  }

  def onRestore[Q <: AnyRef,L <: AnyRef,R <: AnyRef,W <: AnyRef](entityType: CrudEntityType[Q,L,R,W], restoreItem: RestoreItem, crudContext: CrudContext) {
    debug("Restoring " + restoreItem.key + " <- " + restoreItem.dataMap)
    val id = restoreItem.key.substring(restoreItem.key.lastIndexOf("#") + 1).toLong
    val writable = entityType.newWritable
    entityType.copyFields(restoreItem.dataMap, writable)

    entityType.withEntityPersistence(crudContext, _.save(Some(id), writable))
    Unit
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
  def writeEntity(key: String, dataMap: Option[Map[String,Any]])
}

case class RestoreItem(key: String, dataMap: Map[String,Any])