package be.florien.ampacheplayer.view.viewmodel

import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.manager.AmpacheConnection
import be.florien.ampacheplayer.manager.DisplayHelper
import be.florien.ampacheplayer.manager.Navigator
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
class ConnectActivityVM : BaseVM() {

    /**
     * Fields
     */
    @Inject lateinit var ampacheConnection: AmpacheConnection
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var displayHelper: DisplayHelper

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
    }

    /**
     * Buttons calls
     */
    fun connect(username: String, password: String) {
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
                            0 -> {
                                navigator.goToPlayer()
                                //todo finish activity
                            }
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