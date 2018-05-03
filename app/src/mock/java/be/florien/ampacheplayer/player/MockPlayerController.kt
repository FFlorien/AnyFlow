package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
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
            .sample(40, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
    override val songNotifier: Subject<Song> = BehaviorSubject.create()

    private var currentTime: Long = 0
    private var totalTime: Long = 0
    private var isPlaying = false

    init {
        songNotifier.onNext(audioQueue.getCurrentSong())
        audioQueue.positionObservable.subscribe {
            if (isPlaying) {
                play()
            }
        }
    }

    override fun isPlaying(): Boolean = isPlaying

    override fun play() {
        songNotifier.onNext(audioQueue.getCurrentSong())
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