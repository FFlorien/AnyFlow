package be.florien.ampacheplayer.view.connect

import android.databinding.Bindable
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.api.AmpacheConnection
import be.florien.ampacheplayer.view.DisplayHelper
import be.florien.ampacheplayer.view.Navigator
import be.florien.ampacheplayer.view.BaseVM
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
class ConnectActivityVM
@Inject constructor(
        private val ampacheConnection: AmpacheConnection,
        private val navigator: Navigator,
        private val displayHelper: DisplayHelper) : BaseVM() {

    /**
     * Fields
     */

    @Bindable
    var username: String = ""
    @Bindable
    var password: String = ""


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
        if (username.isBlank()) {
            subscribe(ampacheConnection.ping().subscribeOn(Schedulers.io()),
                    {
                        when (it.error.code) {
                            0 -> navigator.goToPlayer()
                            else -> displayHelper.notifyUserAboutError("Impossible de prolonger la session")
                        }
                    },
                    {
                        Timber.e("Error while extending session", it)
                    })
        } else {
            subscribe(
                    ampacheConnection.authenticate(username, password).subscribeOn(Schedulers.io()),
                    {
                        when (it.error.code) {
                            0 -> navigator.goToPlayer() //todo finish activity
                            else -> displayHelper.notifyUserAboutError("Impossible de se connecter avec les informations données")
                        }
                    },
                    {
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