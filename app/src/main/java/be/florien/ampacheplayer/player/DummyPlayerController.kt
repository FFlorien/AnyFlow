package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.business.realm.Song
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

/**
 * Created by florien on 6/08/17.
 */
class DummyPlayerController : PlayerController {
    override val playTimeNotifier: Observable<Int> = Observable
            .interval(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .map { it.toInt() }
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