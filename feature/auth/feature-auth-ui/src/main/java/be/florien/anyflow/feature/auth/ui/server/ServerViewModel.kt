package be.florien.anyflow.feature.auth.ui.server

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistence
import be.florien.anyflow.feature.auth.domain.repository.ServerValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
class ServerViewModel : BaseViewModel() {
    @Inject
    lateinit var authPersistence: AuthPersistence

    @Inject
    lateinit var serverValidator: ServerValidator


    /**
     * Fields
     */
    val urlStatus: LiveData<ServerValidator.ServerStatus?> = MutableLiveData<ServerValidator.ServerStatus?>(null)

    /**
     * Buttons calls
     */
    fun connect(serverUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val serverValid = serverValidator.isServerValid(serverUrl)
            if (serverValid is ServerValidator.ServerStatus.Success) {
                authPersistence.saveServerInfo(serverValid.url)
            }
            urlStatus.mutable.postValue(serverValid)
        }
    }

    fun messageRead() {
        urlStatus.mutable.postValue(null)
    }
}