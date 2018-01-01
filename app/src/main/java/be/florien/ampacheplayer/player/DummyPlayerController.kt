package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.persistence.model.Song
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

/**
 * Player used to test the implementation, but that play nothing
 */
class DummyPlayerController : PlayerController {
    override val playTimeNotifier: Observable<Long> = Observable
            .interval(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
    override val songNotifier: Subject<Song> = BehaviorSubject.create()

    init {
        songNotifier.onNext(Song())
    }

    override fun isPlaying(): Boolean = false

    override fun play() {}

    override fun stop() {}

    override fun pause() {}

    override fun resume() {}
}