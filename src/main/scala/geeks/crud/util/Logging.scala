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
    Log.d(this.getClass.getName, f)
  }

  def info(f: => String) {
    Log.i(this.getClass.getName, f)
  }
}