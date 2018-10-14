package be.florien.anyflow.view.player

import android.content.ComponentName
import android.content.ServiceConnection
import android.databinding.Bindable
import android.os.IBinder
import be.florien.anyflow.BR
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.player.*
import be.florien.anyflow.view.BaseVM
import be.florien.anyflow.view.customView.PlayerControls
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.absoluteValue

/**
 * ViewModel for the PlayerActivity
 */
@ActivityScope
class PlayerActivityVM
@Inject
constructor(private val playingQueue: PlayingQueue, private val libraryDatabase: LibraryDatabase) : BaseVM(), PlayerControls.OnActionListener {

    companion object {
        const val PLAYING_QUEUE_CONTAINER = "PlayingQueue"
        const val PLAYER_CONTROLLER_CONTAINER = "PlayerController"
    }

    internal val connection: PlayerConnection = PlayerConnection()
    private var isBackKeyPreviousSong: Boolean = false
    @Bindable
    var shouldShowBuffering: Boolean = false
    @Bindable
    var playerState: PlayerController.State = PlayerController.State.NO_MEDIA

    var player: PlayerController = IdlePlayerController()
        set(value) {
            dispose(PLAYER_CONTROLLER_CONTAINER)
            field = value
            subscribe(
                    field.stateChangeNotifier.subscribeOn(AndroidSchedulers.mainThread()),
                    onNext = {
                        playerState = it
                        shouldShowBuffering = it ==PlayerController.State.BUFFER
                        notifyPropertyChanged(BR.playerState)
                        notifyPropertyChanged(BR.shouldShowBuffering)
                    },
                    onError = {
                        Timber.e(it, "error while retrieving the state")
                    },
                    containerKey = PLAYER_CONTROLLER_CONTAINER)
            subscribe(
                    observable = player.playTimeNotifier,
                    onNext = {
                        currentDuration = it.toInt()
                        isBackKeyPreviousSong = currentDuration < 10000
                        notifyPropertyChanged(BR.currentDuration)
                    },
                    onError = {
                        Timber.e(it, "error while retrieving the playtime")
                    },
                    containerKey = PLAYER_CONTROLLER_CONTAINER)
        }

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
        subscribe(
                flowable = playingQueue.orderingUpdater,
                onNext = {
                    isOrderRandom = it
                    notifyPropertyChanged(BR.isOrderRandom)
                },
                containerKey = PLAYING_QUEUE_CONTAINER)
        subscribe(
                flowable = playingQueue.currentSongUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    totalDuration = (it?.time ?: 0) * 1000
                    notifyPropertyChanged(BR.totalDuration)
                },
                containerKey = PLAYING_QUEUE_CONTAINER)
        subscribe(
                observable = playingQueue.positionUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    notifyPropertyChanged(BR.nextPossible)
                    notifyPropertyChanged(BR.previousPossible)
                    notifyPropertyChanged(BR.currentDuration)
                },
                containerKey = PLAYING_QUEUE_CONTAINER)
        subscribe(
                flowable = libraryDatabase.getFilters().observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    isFiltered = it.isNotEmpty()
                    notifyPropertyChanged(BR.isFiltered)
                },
                containerKey = PLAYING_QUEUE_CONTAINER
        )
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
        if ((currentDuration - newDuration).absoluteValue > 1000) {
            player.seekTo(newDuration)
        }
    }

    fun randomOrder() {
        subscribe(libraryDatabase.setOrders(listOf(Order(0, Subject.ALL).toDbOrder())))
    }

    fun classicOrder() {
        subscribe(libraryDatabase.setOrdersSubject(listOf(Subject.ALBUM_ARTIST, Subject.YEAR, Subject.ALBUM, Subject.TRACK, Subject.TITLE)))
    }

    /**
     * Bindables
     */

    @Bindable
    var currentDuration: Int = 0

    @Bindable
    var totalDuration: Int = 0

    @Bindable
    var isOrderRandom: Boolean = false

    @Bindable
    var isFiltered: Boolean = false

    @Bindable
    fun isNextPossible(): Boolean = playingQueue.listPosition < playingQueue.itemsCount - 1

    @Bindable
    fun isPreviousPossible(): Boolean = playingQueue.listPosition != 0 || currentDuration > 10000

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
}