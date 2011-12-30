package com.github.scala.android.crud.view

import android.net.Uri
import android.widget.{Toast, ImageView}
import com.github.scala.android.crud.res.R
import com.github.triangle.PortableField._
import android.view.View
import android.content.Intent
import java.io.File
import android.os.Environment
import android.provider.MediaStore
import com.github.triangle.&&
import com.github.scala.android.crud.action.{StartActivityForResultOperation, OperationResponse}
import com.github.triangle.{Getter, GetterFromItem, Field}
import com.github.scala.android.crud.common.PlatformTypes.ImgKey

/** A ViewField for an image that can be captured using the camera.
  * It currently puts the image into external storage, which requires the following in the AndroidManifest.xml:
  * {{{<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />}}}
  * @author Eric Pabst (epabst@gmail.com)
  */
object CapturedImageView extends ViewField[Uri](CapturedImageViewFactory.defaultLayout, CapturedImageViewFactory.dataField) {
  val DefaultValueTagKey: ImgKey = CapturedImageViewFactory.DefaultValueTagKey
}

/** Just a convenient place to put code used to instantiate the CapturedImageView object. */
private object CapturedImageViewFactory {
  val defaultLayout = new FieldLayout {
    def displayXml = <ImageView android:adjustViewBounds="true"/>

    def editXml = <ImageView android:adjustViewBounds="true" android:clickable="true"/>
  }

  def setImageUri(imageView: ImageView, uriOpt: Option[Uri]) {
    Toast.makeText(imageView.getContext, "setting uri on image to " + uriOpt, Toast.LENGTH_LONG).show()
    imageView.setImageBitmap(null)
    uriOpt match {
      case Some(uri) =>
        imageView.setTag(uri.toString)
        imageView.setImageURI(uri)
      case None =>
        imageView.setImageResource(R.drawable.android_camera_256)
    }
  }

  def tagToUri(tag: Object): Option[Uri] = Option(tag.asInstanceOf[String]).map(Uri.parse(_))

  def imageUri(imageView: ImageView): Option[Uri] = tagToUri(imageView.getTag)

  object OperationResponseExtractor extends Field(identityField[OperationResponse])
  object ViewExtractor extends Field(identityField[View])

  // This could be any value.  Android requires that it is some entry in R.
  val DefaultValueTagKey = R.drawable.icon

  val dataField = Getter((v: ImageView) => imageUri(v)).withSetter(v => uri => setImageUri(v, uri)) +
    OnClickOperationSetter(view => StartActivityForResultOperation(view, {
      val intent = new Intent("android.media.action.IMAGE_CAPTURE")
      val imageUri = Uri.fromFile(File.createTempFile("image", ".jpg", Environment.getExternalStorageDirectory))
      intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
      view.setTag(DefaultValueTagKey, imageUri.toString)
      Toast.makeText(view.getContext, "set proposed uri to " + imageUri, Toast.LENGTH_SHORT).show()
      intent
    })) + GetterFromItem {
      case OperationResponseExtractor(Some(response)) && ViewExtractor(Some(view)) =>
        Toast.makeText(view.getContext, "getting uri from result", Toast.LENGTH_SHORT).show()
        Option(response.intent).map(_.getData).orElse(tagToUri(view.getTag(DefaultValueTagKey)))
    }
}
