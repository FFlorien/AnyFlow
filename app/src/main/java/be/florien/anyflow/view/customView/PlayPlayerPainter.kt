package be.florien.anyflow.view.customView

import android.content.Context


internal class PlayPlayerPainter(context: Context, playPauseIconAnimator: PlayPauseIconAnimator) : DurationPlayerPainter(context, playPauseIconAnimator) {

    var playingDuration: Int
        get() = duration
        set(value) {
            this.duration = value
        }

    override fun getButtonClicked(lastDownEventX: Int, downEventX: Int) =
            if (lastDownEventX in 0..playButtonLeftBound && downEventX in 0..playButtonLeftBound) {
                CLICK_PREVIOUS
            } else if (lastDownEventX > playButtonRightBound && downEventX > playButtonRightBound) {
                CLICK_NEXT
            } else if (lastDownEventX in playButtonLeftBound..playButtonRightBound && downEventX in playButtonLeftBound..playButtonRightBound) {
                CLICK_PLAY_PAUSE
            } else {
                CLICK_UNKNOWN
            }

    override fun computePlayPauseIcon() {
        playPauseIconAnimator.computeIcon(currentState, playPausePosition)
    }
}