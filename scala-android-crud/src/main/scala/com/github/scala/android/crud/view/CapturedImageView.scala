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
import com.github.scala.android.crud.common.PlatformTypes.ImgKey
import com.github.triangle._
import com.github.scala.android.crud.GeneratedCrudType.CrudContextField
import com.github.scala.android.crud.action.{ContextVar, ContextVars, StartActivityForResultOperation, OperationResponse}
import com.github.scala.android.crud.common.CachedFunction
import com.github.scala.android.crud.common.Common.withCloseable
import android.graphics.BitmapFactory
import android.graphics.drawable.{BitmapDrawable, Drawable}

/** A ViewField for an image that can be captured using the camera.
  * It currently puts the image into external storage, which requires the following in the AndroidManifest.xml:
  * {{{<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />}}}
  * @author Eric Pabst (epabst@gmail.com)
  */
object CapturedImageView extends ViewField[Uri](new FieldLayout {
  def displayXml = <ImageView android:adjustViewBounds="true"/>
  def editXml = <ImageView android:adjustViewBounds="true" android:clickable="true"/>
}) {
  private object DrawableByUriCache extends ContextVar[CachedFunction[Uri,Drawable]]

  private def bitmapFactoryOptions = {
    val options = new BitmapFactory.Options
    options.inDither = true
    //todo make this depend on the actual image's dimensions
    options.inSampleSize = 4
    options
  }

  private def setImageUri(imageView: ImageView, uriOpt: Option[Uri], contextVars: ContextVars) {
    Toast.makeText(imageView.getContext, "setting uri on image to " + uriOpt, Toast.LENGTH_LONG).show()
    imageView.setImageBitmap(null)
    uriOpt match {
      case Some(uri) =>
        imageView.setTag(uri.toString)
        val contentResolver = imageView.getContext.getContentResolver
        val cachingResolver = DrawableByUriCache.getOrSet(contextVars, CachedFunction(uri => {
          withCloseable(contentResolver.openInputStream(uri)) { stream =>
            new BitmapDrawable(BitmapFactory.decodeStream(stream, null, bitmapFactoryOptions))
          }
        }))
        imageView.setImageDrawable(cachingResolver(uri))
      case None =>
        imageView.setImageResource(R.drawable.android_camera_256)
    }
  }

  private def tagToUri(tag: Object): Option[Uri] = Option(tag.asInstanceOf[String]).map(Uri.parse(_))

  private def imageUri(imageView: ImageView): Option[Uri] = tagToUri(imageView.getTag)

  private object OperationResponseExtractor extends Field(identityField[OperationResponse])
  private object ViewExtractor extends Field(identityField[View])

  // This could be any value.  Android requires that it is some entry in R.
  val DefaultValueTagKey = R.drawable.icon

  protected val delegate = Getter((v: ImageView) => imageUri(v)) + SetterUsingItems[Uri] {
    case (ViewExtractor(Some(view: ImageView)), CrudContextField(Some(crudContext))) => uri =>
      setImageUri(view, uri, crudContext.vars)
  } + OnClickOperationSetter(view => StartActivityForResultOperation(view, {
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
