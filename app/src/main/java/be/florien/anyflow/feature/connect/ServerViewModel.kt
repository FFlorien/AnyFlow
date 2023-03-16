package be.florien.anyflow.feature.connect

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.ServerComponentContainer
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.feature.BaseViewModel
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
        val serverUrl = server.value ?: return //todo checks and warn user
        (context?.applicationContext as ServerComponentContainer).createUserScopeForServer(serverUrl)
        authPersistence.saveServerInfo(serverUrl)
        isServerSetup.mutable.value = true
    }

    override fun onCleared() {
        super.onCleared()
        context = null
    }
}