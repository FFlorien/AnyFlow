package be.florien.anyflow.feature.connect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.R
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.MutableValueLiveData
import be.florien.anyflow.feature.ValueLiveData
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
open class ConnectViewModel
@Inject constructor(
        private val ampacheConnection: AmpacheConnection) : BaseViewModel() {

    /**
     * Fields
     */

    var server = MutableLiveData("")
    var username = MutableLiveData("")
    var password = MutableLiveData("")
    val isLoading: LiveData<Boolean> = MutableValueLiveData(false)
    val isConnected: ValueLiveData<Boolean> = MutableValueLiveData(false)
    val errorMessage: LiveData<Int> = MutableLiveData<Int>(null)

    /**
     * Buttons calls
     */
    fun connect() {
        isLoading.mutable.value = true
        val serverUrl = server.value
                ?: //todo warn user
                return
        ampacheConnection.openConnection(serverUrl)
        if (username.value?.isBlank() == true) {
            subscribe(ampacheConnection.ping().subscribeOn(Schedulers.io()),
                    {
                        isLoading.mutable.value = false
                        when (it.error.code) {
                            0 -> isConnected.mutable.value = true
                            else -> errorMessage.mutable.value = R.string.connect_error_extends
                        }
                    },
                    {
                        isLoading.mutable.value = false
                        this@ConnectViewModel.eLog(it, "Error while extending session")
                    })
        } else {
            val password1 = password.value
            val user = username.value
            if (password1?.isNotBlank() == true && user?.isNotBlank() == true) {
                subscribe(
                        ampacheConnection.authenticate(user, password1).subscribeOn(Schedulers.io()),
                        {
                            isLoading.mutable.value = false
                            when (it.error.code) {
                                0 -> isConnected.mutable.value = true
                                else -> errorMessage.mutable.value = R.string.connect_error_credentials
                            }
                        },
                        {
                            isLoading.mutable.value = false
                            when (it) {
                                is WrongIdentificationPairException -> {
                                    this@ConnectViewModel.eLog(it, "Wrong username/password")
                                    errorMessage.mutable.value = R.string.connect_error_credentials
                                }
                            }
                        })
            }
        }
    }
}