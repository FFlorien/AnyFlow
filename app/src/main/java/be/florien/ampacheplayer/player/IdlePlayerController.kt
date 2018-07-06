package be.florien.ampacheplayer.player

import be.florien.ampacheplayer.persistence.local.model.Song
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

/**
 * Player used to fill the implementation, but that play nothing
 */
class IdlePlayerController : PlayerController {

    override val playTimeNotifier: Observable<Long> = Observable
            .interval(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())

    override fun isPlaying(): Boolean = false

    override fun play() {}

    override fun play(song: Song) {}

    override fun stop() {}

    override fun pause() {}

    override fun resume() {}

    override fun seekTo(duration: Int) {}
}