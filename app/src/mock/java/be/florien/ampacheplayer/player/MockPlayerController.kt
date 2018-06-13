package be.florien.ampacheplayer.player

import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Player used to test the implementation, but that play nothing
 */
class MockPlayerController
@Inject constructor(private val audioQueue: AudioQueue) : PlayerController {
    override fun seekTo(duration: Int) {
        currentTime = duration.toLong()
    }

    override val playTimeNotifier: Observable<Long> = Observable
            .interval(1, TimeUnit.MILLISECONDS)
            .map { currentTime }
            .doOnNext { if (isPlaying) {
                if (currentTime < totalTime) {
                    currentTime ++
                } else {
                    audioQueue.listPosition++
                }
            }}
//            .sample(40, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()

    private var currentTime: Long = 0
    private var totalTime: Long = 0
    private var isPlaying = false

    init {
        audioQueue.positionObservable.subscribe {
            if (isPlaying) {
                play()
            }
        }
    }

    override fun isPlaying(): Boolean = isPlaying

    override fun play() {
        currentTime = 0
        resume()
    }

    override fun stop() {
        isPlaying = false
        currentTime = 0
    }

    override fun pause() {
        isPlaying = false
    }

    override fun resume() {
        totalTime = (audioQueue.getCurrentSong().time * 1000).toLong()
        isPlaying = true
    }
}