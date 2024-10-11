package be.florien.anyflow.ui.server

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistence
import be.florien.anyflow.ui.di.UserVmInjectorContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
open class ServerViewModel @Inject constructor() : BaseViewModel() {

    @SuppressLint("StaticFieldLeak")
    @set:Inject
    var context: Context? = null

    @Inject
    lateinit var authPersistence: AuthPersistence

    val isServerSetup: LiveData<Boolean> = MutableLiveData(false)

    /**
     * Fields
     */

    var server = MutableLiveData("")

    /**
     * Buttons calls
     */
    fun connect() {
        val serverUrl = server.value ?: return
        val userVmInjectorContainer = context?.applicationContext as UserVmInjectorContainer
        viewModelScope.launch(Dispatchers.IO) {
            if (userVmInjectorContainer.createServerComponentIfServerValid(serverUrl)) {
                authPersistence.saveServerInfo(serverUrl)
                isServerSetup.mutable.postValue(true)
            } else {
                //todo warn user
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        context = null
    }
}