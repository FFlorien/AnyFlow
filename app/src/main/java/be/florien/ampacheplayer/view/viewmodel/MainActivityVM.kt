package be.florien.ampacheplayer.view.viewmodel

import android.databinding.BaseObservable
import android.databinding.Bindable
import android.media.AudioManager
import android.media.MediaPlayer
import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.databinding.ActivityMainBinding
import be.florien.ampacheplayer.model.manager.AmpacheConnection
import be.florien.ampacheplayer.model.manager.DataManager
import com.android.databinding.library.baseAdapters.BR
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
class MainActivityVM(val binding: ActivityMainBinding) : BaseObservable() {

    /**
     * Fields
     */
    @Inject lateinit var ampacheConnection: AmpacheConnection
    @Inject lateinit var dataManager: DataManager

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
        ampacheConnection
                .authenticate(binding.inputUsername.text.toString(), binding.inputPassword.text.toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{
                    notifyPropertyChanged(BR.functionEnabled)
                    notifyPropertyChanged(BR.authenticationEnabled)
                }
    }

    fun getSongAndPlay() {
        dataManager
                .getSongs()
                .subscribeOn(Schedulers.io())
                .subscribe {
                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mediaPlayer.setDataSource(it[0].url)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
    }

    fun getArtistAndInfo() {
        dataManager
                .getArtists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    binding.info.text = root.size.toString()
                }
    }

    fun getAlbumAndInfo() {
        dataManager
                .getAlbums()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    binding.info.text = root.size.toString()
                }
    }

    fun getTagAndInfo() {
        dataManager
                .getTags()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    binding.info.text = root.size.toString()
                }
    }

    fun getPlaylistAndInfo() {
        dataManager
                .getPlaylists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    binding.info.text = root.size.toString()
                }
    }
    /**
     * Buttons states
     */
    @Bindable
    fun getAuthenticationEnabled() : Boolean {
        return ampacheConnection.authSession.isEmpty()
    }

    @Bindable
    fun getFunctionEnabled() : Boolean {
        return ampacheConnection.authSession.isNotEmpty()
    }
}