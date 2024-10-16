package be.florien.anyflow.feature.auth.ui.server

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistence
import be.florien.anyflow.feature.auth.domain.repository.ServerValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
open class ServerViewModel : BaseViewModel() {
    @Inject
    lateinit var authPersistence: AuthPersistence

    @Inject
    lateinit var serverValidator: ServerValidator


    /**
     * Fields
     */

    val server = MutableLiveData("")
    val validatedServerUrl = MutableLiveData("")

    /**
     * Buttons calls
     */
    fun connect() {
        val serverUrl = server.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (serverValidator.isServerValid(serverUrl)) {
                authPersistence.saveServerInfo(serverUrl)
                validatedServerUrl.mutable.postValue(serverUrl)
            } else {
                //todo warn user (see ServerValidator)
            }
        }
    }
}