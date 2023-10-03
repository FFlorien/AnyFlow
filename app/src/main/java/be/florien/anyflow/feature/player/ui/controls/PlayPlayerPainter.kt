package be.florien.anyflow.feature.player.ui.controls

import android.content.Context


internal class PlayPlayerPainter(
    context: Context,
    playPauseIconAnimator: PlayPauseIconAnimator,
    previousIconAnimator: PreviousIconAnimator
) : PlayerPainter(context, playPauseIconAnimator, previousIconAnimator) {

    var playingDuration: Int
        get() = duration
        set(value) {
            this.duration = value
        }

    override fun getButtonClicked(lastDownEventX: Int, downEventX: Int): Int {
        val buttonLeftBound = playButtonLeftBound.toInt()
        val buttonRightBound = playButtonRightBound.toInt()
        return if (lastDownEventX in 0..buttonLeftBound && downEventX in 0..buttonLeftBound) {
            CLICK_PREVIOUS
        } else if (lastDownEventX > playButtonRightBound && downEventX > playButtonRightBound) {
            CLICK_NEXT
        } else if (lastDownEventX in buttonLeftBound..buttonRightBound && downEventX in buttonLeftBound..buttonRightBound) {
            CLICK_PLAY_PAUSE
        } else {
            CLICK_UNKNOWN
        }
    }

    override fun computePlayPauseIcon() {
        playPauseIconAnimator.computeIcon(currentState, playPausePosition)
    }
}