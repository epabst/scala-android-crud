package info.kghost.example.android.scala

import android.app.Activity
import android.os.Bundle

class TestActivity extends Activity {
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
  }
}
