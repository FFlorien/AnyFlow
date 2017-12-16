package be.florien.ampacheplayer.view.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.view.viewmodel.ConnectActivityVM

/**
 * Simple activity for connection
 */
class ConnectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConnectActivityVM(this)
    }
}