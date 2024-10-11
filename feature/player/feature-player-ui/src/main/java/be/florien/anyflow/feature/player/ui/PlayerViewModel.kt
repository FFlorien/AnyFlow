package be.florien.anyflow.feature.player.ui

import android.content.ComponentName
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.component.player.controls.PlayPauseIconAnimator
import be.florien.anyflow.component.player.controls.PlayerControls
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.management.alarm.AlarmsSynchronizer
import be.florien.anyflow.management.podcast.PodcastPersistence
import be.florien.anyflow.management.podcast.PodcastRepository
import be.florien.anyflow.management.queue.PlayingQueue
import be.florien.anyflow.management.waveform.WaveFormRepository
import be.florien.anyflow.tags.DataRepository
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    private val alarmsSynchronizer: AlarmsSynchronizer,
    private val waveFormRepository: WaveFormRepository,
    private val dataRepository: DataRepository,
    private val podcastRepository: PodcastRepository,
    private val podcastPersistence: PodcastPersistence,
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
    val currentDuration: StateFlow<Int> = MutableStateFlow(0)
    val totalDuration: LiveData<Int> = playingQueue//todo awful
        .currentMedia
        .asFlow()
        .map { queueItem ->
            if (queueItem == null) {
                0
            } else if (queueItem.mediaType == PODCAST_MEDIA_TYPE) {
                queueItem.id.let { podcastRepository.getPodcastDuration(it) } * 1000
            } else {
                queueItem.id.let { dataRepository.getSongDuration(it) } * 1000
            }
        }
        .asLiveData()

    val isPreviousPossible: LiveData<Boolean> = playingQueue.positionUpdater.map { it != 0 }
    val waveForm: LiveData<DoubleArray> =
        playingQueue.currentMedia.switchMap {
            if (it?.mediaType == SONG_MEDIA_TYPE) {
                it.let { waveFormRepository.getComputedWaveForm(it.id, MediaMetadata.MEDIA_TYPE_MUSIC) }
            } else {
                it.let { waveFormRepository.getComputedWaveForm(it?.id ?: 0L, MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE) }
            }
        }
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
                if (
                    playingQueue.currentMedia.value?.mediaType == PODCAST_MEDIA_TYPE
                    && (currentDuration.value % 10000) < 10
                    && (currentDuration.value / 1000) > 10
                    && getFromPlayer(false) { isPlaying }
                ) {
                    playingQueue.currentMedia.value?.id?.let { podcastId ->
                        podcastPersistence.savePodcastPosition(
                            podcastId,
                            (currentDuration.value).toLong()
                        )
                    }
                }
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
            playbackState.mutable.emit(
                if (playWhenReady) {
                    PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY
                } else {
                    PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
                }
            )
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

    fun <T> MutableLiveData<T>.postValueIfChanged(newValue: T) {
        if (newValue != value){
            postValue(newValue)
        }
    }
}