package be.florien.anyflow.feature.auth.ui.user

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.architecture.di.ViewModelFactoryProvider
import be.florien.anyflow.feature.auth.ui.R
import be.florien.anyflow.feature.auth.ui.databinding.ActivityConnectBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Simple activity for connection
 */
@SuppressLint("Registered")
open class UserConnectActivityBase : AppCompatActivity() {
    lateinit var viewModel: UserConnectViewModel
    internal lateinit var binding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            (application as ViewModelFactoryProvider).viewModelFactory
        )[UserConnectViewModel::class.java]

        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.isConnected.observe(this) {
            if (it) {
                viewModel.navigator.navigateToMain(this)
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