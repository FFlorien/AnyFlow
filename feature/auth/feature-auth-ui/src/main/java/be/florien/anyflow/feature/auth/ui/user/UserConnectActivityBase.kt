package be.florien.anyflow.feature.auth.ui.user

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.feature.auth.ui.R
import be.florien.anyflow.feature.auth.ui.databinding.ActivityConnectBinding
import be.florien.anyflow.feature.auth.ui.di.UserConnectActivityComponentCreator
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject

/**
 * Simple activity for connection
 */
@SuppressLint("Registered")
open class UserConnectActivityBase : AppCompatActivity() {

    @Inject
    lateinit var viewModelProvider: AnyFlowViewModelFactory

    lateinit var viewModel: UserConnectViewModel
    internal lateinit var binding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userConnectActivityComponent =
            (application as UserConnectActivityComponentCreator).createUserConnectComponent()
                ?: throw IllegalStateException()
        userConnectActivityComponent.inject(this)

        viewModel = ViewModelProvider(this, viewModelProvider)[UserConnectViewModel::class.java]

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