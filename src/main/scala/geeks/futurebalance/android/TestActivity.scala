package geeks.futurebalance.android

import android.app.Activity
import android.os.Bundle

class TestActivity extends Activity {
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
  }
}
