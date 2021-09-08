package be.florien.anyflow.feature.player

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.*
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.view.Order
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_ALBUM
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_ALBUM_ARTIST
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_ALL
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_TITLE
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_TRACK
import be.florien.anyflow.data.view.Order.Companion.SUBJECT_YEAR
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.customView.PlayPauseIconAnimator
import be.florien.anyflow.feature.customView.PlayerControls
import be.florien.anyflow.player.IdlePlayerController
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.player.PlayingQueue
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
        private val dataRepository: DataRepository,
        val connectionStatus: LiveData<AmpacheConnection.ConnectionStatus>,
        @Named("Songs")
        val songsUpdatePercentage: LiveData<Int>,
        @Named("Albums")
        val albumsUpdatePercentage: LiveData<Int>,
        @Named("Artists")
        val artistsUpdatePercentage: LiveData<Int>) : BaseViewModel(), PlayerControls.OnActionListener {

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
    val totalDuration: LiveData<Int> = playingQueue.currentSong.map { it.time * 1000 }

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
            player.play()
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
        if ((currentDuration.value?.minus(newDuration))?.absoluteValue ?: 0 > 1000) {
            player.seekTo(newDuration)
        }
    }

    fun randomOrder() {
        viewModelScope.launch {
            val orders = mutableListOf(Order(0, SUBJECT_ALL))
            dataRepository.setOrders(orders)
        }
    }

    fun classicOrder() {
        viewModelScope.launch {
            dataRepository.setOrdersSubject(listOf(SUBJECT_ALBUM_ARTIST, SUBJECT_YEAR, SUBJECT_ALBUM, SUBJECT_TRACK, SUBJECT_TITLE))
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