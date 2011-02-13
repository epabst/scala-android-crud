package geeks.crud.util

import android.util.Log

/**
 * A trait to enable logging
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 6:54 AM
 */

trait Logging {
  def debug(f: => String) {
    try Log.d(this.getClass.getName, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }

  def info(f: => String) {
    try Log.i(this.getClass.getName, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }
}