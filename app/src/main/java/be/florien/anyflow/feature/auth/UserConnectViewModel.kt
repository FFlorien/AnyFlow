package be.florien.anyflow.feature.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
open class UserConnectViewModel : BaseViewModel() {

    @Inject
    lateinit var authRepository: AuthRepository

    /**
     * Fields
     */

    val username = MutableLiveData("")
    val password = MutableLiveData("")
    val apiToken = MutableLiveData("")
    val isLoading: LiveData<Boolean> = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = MutableLiveData(false)
    val errorMessage: LiveData<Int> = MutableLiveData(-1)

    /**
     * Buttons calls
     */
    fun connect() {
        isLoading.mutable.value = true
        val pwd = password.value
        val user = username.value
        val apiToken = apiToken.value
        val isUserPwd = pwd?.isNotBlank() == true && user?.isNotBlank() == true //todo : what if everything is filled ?
        if (isUserPwd || apiToken?.isNotBlank() == true) {
            viewModelScope.launch {
                try {
                    val it = if (isUserPwd) {
                        authRepository.authenticate(user!!, pwd!!)
                    } else {
                        authRepository.authenticate(apiToken!!)
                    }
                    isLoading.mutable.value = false
                    when (it.error.errorCode) {
                        0 -> isConnected.mutable.value = true
                        else -> errorMessage.mutable.value = R.string.connect_error_credentials
                    }
                } catch (it: Exception) {
                    isLoading.mutable.value = false
                    this@UserConnectViewModel.eLog(it)
                    when (it) {
                        is WrongIdentificationPairException -> {
                            errorMessage.mutable.value = R.string.connect_error_credentials
                        }

                        else -> this@UserConnectViewModel.eLog(it, "Connection failed")
                    }
                }
            }
        }
    }
}