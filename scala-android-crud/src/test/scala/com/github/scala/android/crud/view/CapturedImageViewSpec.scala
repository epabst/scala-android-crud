package com.github.scala.android.crud.view

import org.junit.runner.RunWith
import org.scalatest.matchers.MustMatchers
import ViewField._
import android.view.View
import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.Test
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import com.github.scala.android.crud.action.OperationResponse
import android.net.Uri
import android.content.Intent

/** A behavior specification for [[com.github.scala.android.crud.view.ViewField]].
  * @author Eric Pabst (epabst@gmail.com)
  */

@RunWith(classOf[RobolectricTestRunner])
class CapturedImageViewSpec extends MustMatchers with MockitoSugar {
  @Test
  def capturedImageViewMustGetImageUriFromOperationResponse() {
    val uri = Uri.parse("file://foo/bar.jpg")
    val TheViewId = 101
    val field = viewId(TheViewId, CapturedImageView)
    val outerView = mock[View]
    val view = mock[View]
    val intent = mock[Intent]
    stub(outerView.getId).toReturn(TheViewId)
    stub(outerView.findViewById(TheViewId)).toReturn(view)
    stub(intent.getData).toReturn(uri)
    stub(view.getTag(CapturedImageView.DefaultValueTagKey)).toReturn(uri.toString)
    field.getterFromItem(List(OperationResponse(TheViewId, intent), outerView)) must be (Some(uri))
  }

  @Test
  def capturedImageViewMustGetImageUriFromViewTagOperationResponseDoesNotHaveIt() {
    val TheViewId = 101
    val field = viewId(TheViewId, ViewField.capturedImageView)
    val outerView = mock[View]
    val view = mock[View]
    stub(outerView.getId).toReturn(TheViewId)
    stub(outerView.findViewById(TheViewId)).toReturn(view)
    stub(view.getTag(CapturedImageView.DefaultValueTagKey)).toReturn("file://foo/bar.jpg")
    field.getterFromItem(List(OperationResponse(TheViewId, null), outerView)) must be (Some(Uri.parse("file://foo/bar.jpg")))
  }
}
