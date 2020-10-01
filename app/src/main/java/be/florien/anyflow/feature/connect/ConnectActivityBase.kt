package be.florien.anyflow.feature.connect

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ActivityConnectBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.feature.player.PlayerActivity
import com.google.android.material.snackbar.Snackbar

/**
 * Simple activity for connection
 */
@SuppressLint("Registered")
open class ConnectActivityBase : AppCompatActivity() {
    lateinit var viewModel: ConnectViewModel
    internal lateinit var binding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(ConnectViewModel::class.java)
        anyFlowApp.applicationComponent.inject(viewModel)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.isConnected.observe(this) {
            if (it) {
                startActivity(PlayerActivity::class)
                finish()
            }
        }
        viewModel.errorMessage.observe(this) {
            if (it > 0) {
                Snackbar.make(binding.loadingProgress, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}