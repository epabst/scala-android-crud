package geeks.crud.util

import android.util.Log

/**
 * A trait to enable logging
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 6:54 AM
 */

trait Logging {
  protected def debug(f: => String) {
    println("DEBUG " + f)
    try Log.d(this.getClass.getName, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }

  protected def info(f: => String) {
    println("INFO  " + f)
    try Log.i(this.getClass.getName, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }

  protected def warn(f: => String) {
    println("WARN  " + f)
    try Log.w(this.getClass.getName, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }

  protected def error(f: => String) {
    println("ERROR " + f)
    try Log.e(this.getClass.getName, f)
    catch { case e: RuntimeException if (e.getMessage == "Stub!") => }
  }
}