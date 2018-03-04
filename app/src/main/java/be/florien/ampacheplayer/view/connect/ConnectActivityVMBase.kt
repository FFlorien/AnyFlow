package be.florien.ampacheplayer.view.connect

import android.databinding.Bindable
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.api.AmpacheConnection
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.view.BaseVM
import be.florien.ampacheplayer.view.DisplayHelper
import be.florien.ampacheplayer.view.Navigator
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
@ActivityScope
open class ConnectActivityVMBase
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
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
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
                            else -> displayHelper.notifyUserAboutError("Impossible de prolonger la session")
                        }
                    },
                    {
                        isLoading = false
                        Timber.e("Error while extending session", it)
                    })
        } else {
            subscribe(
                    ampacheConnection.authenticate(username, password).subscribeOn(Schedulers.io()),
                    {
                        isLoading = false
                        when (it.error.code) {
                            0 -> navigator.goToPlayer() //todo finish activity
                            else -> displayHelper.notifyUserAboutError("Impossible de se connecter avec les informations données")
                        }
                    },
                    {
                        isLoading = false
                        when (it) {
                            is WrongIdentificationPairException -> {
                                Timber.e("Wrong username/password", it)
                                displayHelper.notifyUserAboutError("Impossible de se connecter avec les informations données")
                            }
                        }

                    })

        }
    }
}