package be.florien.anyflow.ui.user

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.feature.auth.ui.R
import be.florien.anyflow.feature.auth.ui.databinding.ActivityConnectBinding
import be.florien.anyflow.ui.di.UserVmInjectorContainer
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
            ViewModelProvider.NewInstanceFactory()
        )[UserConnectViewModel::class.java]
        (applicationContext as UserVmInjectorContainer).userVmInjector?.inject(viewModel)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.isConnected.observe(this) {
            if (it) {
                viewModel.navigator.navigateToPlayer(this)
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