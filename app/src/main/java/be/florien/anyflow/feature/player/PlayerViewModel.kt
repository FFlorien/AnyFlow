package be.florien.anyflow.feature.player

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.*
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import be.florien.anyflow.feature.customView.PlayPauseIconAnimator
import be.florien.anyflow.feature.customView.PlayerControls
import be.florien.anyflow.player.*
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
    private val playingQueue: PlayingQueue,
    private val orderComposer: OrderComposer,
    private val alarmsSynchronizer: AlarmsSynchronizer,
    val connectionStatus: LiveData<AmpacheDataSource.ConnectionStatus>,
    @Named("Songs")
        val songsUpdatePercentage: LiveData<Int>,
    @Named("Genres")
        val genresUpdatePercentage: LiveData<Int>,
    @Named("Albums")
        val albumsUpdatePercentage: LiveData<Int>,
    @Named("Artists")
        val artistsUpdatePercentage: LiveData<Int>,
    @Named("Playlists")
        val playlistsUpdatePercentage: LiveData<Int>) : BaseViewModel(), PlayerControls.OnActionListener {

    internal val playerConnection: PlayerConnection = PlayerConnection()
    internal val updateConnection: UpdateConnection = UpdateConnection()
    private var isBackKeyPreviousSong: Boolean = false

    /**
     * Bindables
     */
    val shouldShowBuffering: LiveData<Boolean> = MutableLiveData(false)
    val state: LiveData<Int> = MediatorLiveData()
    val isOrdered: LiveData<Boolean> = playingQueue.isOrderedUpdater

    val currentDuration: LiveData<Int> = MediatorLiveData()
    val totalDuration: LiveData<Int> = playingQueue.currentSong.map { ((it as SongInfo?)?.time ?: 0) * 1000 }

    val isPreviousPossible: LiveData<Boolean> = playingQueue.positionUpdater.map { it != 0 }

    var player: PlayerController = IdlePlayerController()
        set(value) {
            (currentDuration as MediatorLiveData).removeSource(field.playTimeNotifier)
            (state as MediatorLiveData).removeSource(field.stateChangeNotifier)
            field = value
            currentDuration.addSource(field.playTimeNotifier) {
                isBackKeyPreviousSong = it.toInt() < 10000
                currentDuration.mutable.value = it.toInt()
            }
            state.addSource(field.stateChangeNotifier) {
                shouldShowBuffering.mutable.value = it == PlayerController.State.BUFFER
                state.mutable.value = when (it) {
                    PlayerController.State.PLAY -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY
                    PlayerController.State.BUFFER,
                    PlayerController.State.PAUSE -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
                    else -> PlayPauseIconAnimator.STATE_PLAY_PAUSE_BUFFER
                }
            }
        }

    /**
     * PlayerControls.OnActionListener methods
     */
    override fun onPreviousClicked() {
        if (isBackKeyPreviousSong) {
            playingQueue.listPosition -= 1
        } else {
            player.seekTo(0L)
        }
    }

    override fun onNextClicked() {
        playingQueue.listPosition += 1
    }

    override fun onPlayPauseClicked() {
        player.apply {
            if (isPlaying()) {
                pause()
            } else {
                resume()
            }
        }
    }

    override fun onCurrentDurationChanged(newDuration: Long) {
        if (((currentDuration.value?.minus(newDuration))?.absoluteValue ?: 0) > 1000) {
            player.seekTo(newDuration)
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

    /**
     * Inner class
     */
    inner class PlayerConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            player = (service as PlayerService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            player = IdlePlayerController()
        }
    }

    inner class UpdateConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}

        override fun onServiceDisconnected(name: ComponentName?) {}
    }
}