package be.florien.anyflow.feature.connect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
open class ConnectViewModel : BaseViewModel() {

    @Inject
    lateinit var ampacheDataSource: AmpacheDataSource

    /**
     * Fields
     */

    var server = MutableLiveData("")
    var username = MutableLiveData("")
    var password = MutableLiveData("")
    val isLoading: LiveData<Boolean> = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = MutableLiveData(false)
    val errorMessage: LiveData<Int> = MutableLiveData(-1)

    /**
     * Buttons calls
     */
    fun connect() {
        isLoading.mutable.value = true
        val serverUrl = server.value ?: return //todo warn user
        ampacheDataSource.openConnection(serverUrl)
        if (username.value?.isBlank() == true) {
            viewModelScope.launch {

                try {
                    val it = ampacheDataSource.ping()

                    isLoading.mutable.value = false
                    when (it.error.errorCode) {
                        0 -> isConnected.mutable.value = true
                        else -> errorMessage.mutable.value = R.string.connect_error_extends
                    }
                } catch (it: Exception) {
                    isLoading.mutable.value = false
                    this@ConnectViewModel.eLog(it, "Error while extending session")
                }
            }
        } else {
            val password1 = password.value
            val user = username.value
            if (password1?.isNotBlank() == true && user?.isNotBlank() == true) {
                viewModelScope.launch {
                    try {
                        val it = ampacheDataSource.authenticate(user, password1)
                        isLoading.mutable.value = false
                        when (it.error.errorCode) {
                            0 -> isConnected.mutable.value = true
                            else -> errorMessage.mutable.value = R.string.connect_error_credentials
                        }
                    } catch (it: Exception) {
                        isLoading.mutable.value = false
                        this@ConnectViewModel.eLog(it)
                        when (it) {
                            is WrongIdentificationPairException -> {
                                errorMessage.mutable.value = R.string.connect_error_credentials
                            }
                            else -> this@ConnectViewModel.eLog(it, "Connection failed")
                        }
                    }
                }
            }
        }
    }
}