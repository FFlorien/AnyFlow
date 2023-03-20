package be.florien.anyflow.feature.connect

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.ServerComponentContainer
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.feature.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
open class ServerViewModel : BaseViewModel() {

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
        val serverComponentContainer = context?.applicationContext as ServerComponentContainer
        viewModelScope.launch(Dispatchers.IO) {
            if (serverComponentContainer.createServerComponentIfServerValid(serverUrl)) {
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