package be.florien.ampacheplayer.view.viewmodel

import android.app.Activity
import android.content.Intent
import android.databinding.BaseObservable
import android.support.design.widget.Snackbar
import be.florien.ampacheplayer.databinding.ActivityConnectBinding
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.manager.AmpacheConnection
import be.florien.ampacheplayer.view.activity.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
class ConnectActivityVM(val activity: Activity, val binding: ActivityConnectBinding) : BaseObservable() {

    /**
     * Fields
     */
    @Inject lateinit var ampacheConnection: AmpacheConnection

    /**
     * Constructor
     */
    init {
        binding.vm = this
        activity.ampacheApp.applicationComponent.inject(this)
        Timber.tag(this.javaClass.simpleName)
    }

    /**
     * Buttons calls
     */
    fun connect() {
        if (binding.inputUsername.text.isBlank()) {
            ampacheConnection.ping()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        when (it.error.code) {
                            0 -> activity.startActivity(Intent(activity, PlayerActivity::class.java))
                            else -> Snackbar.make(binding.activityMain, "Impossible de prolonger la session", Snackbar.LENGTH_SHORT).show()
                        }
                    }, {
                        Timber.e("Error while extending session", it)
                    })
        } else {
            ampacheConnection
                    .authenticate(binding.inputUsername.text.toString(), binding.inputPassword.text.toString())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        when (it.error.code) {
                            0 -> {
                                activity.startActivity(Intent(activity, PlayerActivity::class.java))
                                activity.finish()
                            }
                            else -> Snackbar.make(binding.activityMain, "Impossible de se connecter avec les informations données", Snackbar.LENGTH_SHORT).show()
                        }
                    }, {
                        when (it) {
                            is WrongIdentificationPairException -> {
                                Timber.e("Wrong username/password", it)
                                Snackbar.make(binding.activityMain, "Impossible de se connecter avec les informations données", Snackbar.LENGTH_SHORT).show()
                            }
                        }

                    })
        }
    }
}