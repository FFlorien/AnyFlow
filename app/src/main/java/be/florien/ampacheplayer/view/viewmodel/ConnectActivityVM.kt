package be.florien.ampacheplayer.view.viewmodel

import android.content.Context
import android.content.Intent
import android.databinding.BaseObservable
import android.media.AudioManager
import android.media.MediaPlayer
import android.support.design.widget.Snackbar
import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.databinding.ActivityConnectBinding
import be.florien.ampacheplayer.manager.AuthenticationManager
import be.florien.ampacheplayer.manager.DataManager
import be.florien.ampacheplayer.view.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
class ConnectActivityVM(val context: Context, val binding: ActivityConnectBinding) : BaseObservable() {

    /**
     * Fields
     */
    @Inject lateinit var authenticationManager: AuthenticationManager

    /**
     * Constructor
     */
    init {
        binding.vm = this
        App.ampacheComponent.inject(this)
    }

    /**
     * Buttons calls
     */
    fun connect() {
        if (binding.inputUsername.text.isBlank()) {
            authenticationManager.extendsSession()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it) {
                            context.startActivity(Intent(context, PlayerActivity::class.java))
                        } else {
                            Snackbar.make(binding.activityMain, "Impossible de prolonger la session", Snackbar.LENGTH_SHORT).show()
                        }
                    }
        } else {
            authenticationManager
                    .authenticate(binding.inputUsername.text.toString(), binding.inputPassword.text.toString())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it) {
                            context.startActivity(Intent(context, PlayerActivity::class.java))
                        } else {
                            Snackbar.make(binding.activityMain, "Impossible de se connecter avec les informations données", Snackbar.LENGTH_SHORT).show()
                        }
                    }
        }
    }
}