package be.florien.anyflow.feature.player.ui

import android.content.ComponentName
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.IBinder
import androidx.lifecycle.*
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.postValueIfChanged
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import be.florien.anyflow.feature.auth.AuthRepository
import be.florien.anyflow.feature.player.services.WaveFormRepository
import be.florien.anyflow.feature.player.services.queue.OrderComposer
import be.florien.anyflow.feature.player.services.queue.PlayingQueue
import be.florien.anyflow.feature.player.ui.controls.PlayPauseIconAnimator
import be.florien.anyflow.feature.player.ui.controls.PlayerControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.absoluteValue

/**
 * ViewModel for the PlayerActivity
 */
class PlayerViewModel
@Inject
constructor(
    playingQueue: PlayingQueue,
    private val orderComposer: OrderComposer,
    private val alarmsSynchronizer: AlarmsSynchronizer,
    private val waveFormRepository: WaveFormRepository,
    val connectionStatus: LiveData<AuthRepository.ConnectionStatus>,
    @Named("Songs")
    val songsUpdatePercentage: LiveData<Int>,
    @Named("Genres")
    val genresUpdatePercentage: LiveData<Int>,
    @Named("Albums")
    val albumsUpdatePercentage: LiveData<Int>,
    @Named("Artists")
    val artistsUpdatePercentage: LiveData<Int>,
    @Named("Playlists")
    val playlistsUpdatePercentage: LiveData<Int>
) : BaseViewModel(), PlayerControls.OnActionListener, Player.Listener {
    //region observable fields
    val hasInternet: LiveData<Boolean> = MutableLiveData()
    val isConnecting = connectionStatus.map { it == AuthRepository.ConnectionStatus.CONNEXION }
    val shouldShowBuffering: LiveData<Boolean> = MutableLiveData(false)
    val playbackState: StateFlow<Int> =
        MutableStateFlow(PlayPauseIconAnimator.STATE_PLAY_PAUSE_BUFFER)
    val isOrdered: LiveData<Boolean> = playingQueue.isOrderedUpdater
    val currentDuration: StateFlow<Int> = MutableStateFlow(0)
    val totalDuration: LiveData<Int> = playingQueue
        .currentSong
        .map { ((it as SongInfo?)?.time ?: 0) * 1000 }

    val isPreviousPossible: LiveData<Boolean> = playingQueue.positionUpdater.map { it != 0 }
    val waveForm: LiveData<DoubleArray> =
        playingQueue.currentSong.switchMap { waveFormRepository.getComputedWaveForm(it.id) }
            .distinctUntilChanged()

    val isSeekable: LiveData<Boolean> = MutableLiveData(false)
    //endregion

    //region public fields
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasNet =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            hasInternet.mutable.postValueIfChanged(hasNet)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            hasInternet.mutable.postValueIfChanged(false)
        }
    }
    internal val updateConnection: UpdateConnection = UpdateConnection()
    var player: MediaController? = null
        set(value) {
            field?.removeListener(this@PlayerViewModel)
            field = value
            value?.addListener(this@PlayerViewModel)

        }
    //endregion

    init {
        viewModelScope.launch(Dispatchers.Default) {//todo safer, look for the lifecycle ?
            while (true) {
                delay(10)
                (currentDuration as MutableStateFlow).emit(
                    getFromPlayer(0) { contentPosition }.toInt()
                )
            }
        }
    }

    // region PlayerControls.OnActionListener methods
    override fun onPreviousClicked() {
        if (currentDuration.value < 10 * 1000) {
            doOnPlayer { seekToPrevious() }
        } else {
            doOnPlayer { seekTo(0L) }
        }
    }

    override fun onNextClicked() {
        doOnPlayer { seekToNext() }
    }

    override fun onPlayPauseClicked() {
        doOnPlayer {
            if (isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    override fun onCurrentDurationChanged(newDuration: Long) {
        doOnPlayer {
            if (
                isCurrentMediaItemSeekable
                && ((contentPosition - newDuration).absoluteValue) > 1000
            ) {
                seekTo(newDuration)
            }
        }
    }

    fun randomOrder() {
        viewModelScope.launch {
            orderComposer.randomize()
        }
    }

    fun classicOrder() {
        viewModelScope.launch {
            orderComposer.order()
        }
    }

    fun syncAlarms() {
        viewModelScope.launch {
            alarmsSynchronizer.syncAlarms()
        }
    }

    //endregion
    override fun onPlaybackStateChanged(playbackState: Int) {
        var iconPlaybackState: Int = PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
        viewModelScope.launch(Dispatchers.Main) {
            when (playbackState) {
                Player.STATE_BUFFERING, Player.STATE_IDLE -> {
                    shouldShowBuffering.mutable.postValueIfChanged(true)
                    iconPlaybackState = PlayPauseIconAnimator.STATE_PLAY_PAUSE_BUFFER
                }

                Player.STATE_READY -> {
                    isSeekable.mutable.postValueIfChanged(getFromPlayer(false) { isCurrentMediaItemSeekable })
                    shouldShowBuffering.mutable.postValueIfChanged(false)
                    iconPlaybackState = if (getFromPlayer(false) { isPlaying }) {
                        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY
                    } else {
                        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
                    }
                }

                else -> {
                    shouldShowBuffering.mutable.postValueIfChanged(false)
                }
            }
            this@PlayerViewModel.playbackState.mutable.emit(iconPlaybackState)
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            playbackState.mutable.emit(if (playWhenReady) {
                PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY
            } else {
                PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
            })
        }
    }

    fun setInternetPresence(hasNet: Boolean) {
        hasInternet.mutable.postValueIfChanged(hasNet)
    }

    private fun doOnPlayer(action: Player.() -> Unit) { //todo put that in a specific class to avoid direct access to player
        viewModelScope.launch(Dispatchers.Main) {
            player?.action()
        }
    }

    private suspend fun <T> getFromPlayer(defaultValue: T, getter: Player.() -> T) =
        viewModelScope.async(Dispatchers.Main) { player?.getter() ?: defaultValue }.await()

    inner class UpdateConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}

        override fun onServiceDisconnected(name: ComponentName?) {}
    }
}