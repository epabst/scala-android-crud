package com.github.scala.android.crud

import android.app.Application

/**
 * A CrudApplication for Android.
 *
 * Because this extends android.app.Application, it can't normally be instantiated
 * except on a device.  Because of this, there is a convention
 * that each CrudApplication will have {{{class MyApplication extends CrudApplication {..} }}} that has its code,
 * then have {{{class MyAndroidApplication extends CrudAndroidApplication(new MyApplication)}}}.
 *
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/2/12
 * Time: 5:07 PM
 */
abstract class CrudAndroidApplication(val application: CrudApplication) extends Application
