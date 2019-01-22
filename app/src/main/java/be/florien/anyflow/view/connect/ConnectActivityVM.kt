package be.florien.anyflow.view.connect

import android.databinding.Bindable
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.persistence.server.AmpacheConnection
import be.florien.anyflow.view.BaseVM
import be.florien.anyflow.view.DisplayHelper
import be.florien.anyflow.view.Navigator
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
@ActivityScope
open class ConnectActivityVM
@Inject constructor(
        private val ampacheConnection: AmpacheConnection,
        private val navigator: Navigator,
        private val displayHelper: DisplayHelper) : BaseVM() {

    /**
     * Fields
     */

    @get:Bindable
    var server: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.server)
        }
    @get:Bindable
    var username: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.username)
        }
    @get:Bindable
    var password: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.password)
        }
    @get:Bindable
    var isLoading: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.loading)
        }

    /**
     * Buttons calls
     */
    fun connect() {
        isLoading = true
        ampacheConnection.openConnection(server)
        if (username.isBlank()) {
            subscribe(ampacheConnection.ping().subscribeOn(Schedulers.io()),
                    {
                        isLoading = false
                        when (it.error.code) {
                            0 -> navigator.goToPlayer()
                            else -> displayHelper.notifyUserAboutError(R.string.connect_error_extends)
                        }
                    },
                    {
                        isLoading = false
                        this@ConnectActivityVM.eLog(it, "Error while extending session")
                    })
        } else {
            subscribe(
                    ampacheConnection.authenticate(username, password).subscribeOn(Schedulers.io()),
                    {
                        isLoading = false
                        when (it.error.code) {
                            0 -> navigator.goToPlayer() //todo finish activity
                            else -> displayHelper.notifyUserAboutError(R.string.connect_error_credentials)
                        }
                    },
                    {
                        isLoading = false
                        when (it) {
                            is WrongIdentificationPairException -> {
                                this@ConnectActivityVM.eLog(it, "Wrong username/password")
                                displayHelper.notifyUserAboutError(R.string.connect_error_credentials)
                            }
                        }

                    })

        }
    }
}