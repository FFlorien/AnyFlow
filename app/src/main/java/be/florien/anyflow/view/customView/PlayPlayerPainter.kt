package be.florien.anyflow.view.customView

import android.content.Context
import be.florien.anyflow.R


internal class PlayPlayerPainter(context: Context) : DurationPlayerPainter(context) {

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


    private var isFirstTime = true
    override fun computePlayPauseIcon() {
        playPauseIcon = if (currentState != oldState) {
            when {
                oldState == PlayerControls.STATE_BUFFER && currentState == PlayerControls.STATE_PLAY -> {
                    //isPlayPauseAnimationInfinite = false
                    getAnimatedIcon(R.drawable.ic_buffer_to_pause, playPausePosition)
                }
                oldState == PlayerControls.STATE_BUFFER && currentState == PlayerControls.STATE_PAUSE -> {
                    //isPlayPauseAnimationInfinite = false
                    getAnimatedIcon(R.drawable.ic_buffer_to_play, playPausePosition)
                }
                oldState == PlayerControls.STATE_PLAY && currentState == PlayerControls.STATE_BUFFER -> {
                    //isPlayPauseAnimationInfinite = true
                    getAnimatedIcon(R.drawable.ic_pause_to_buffer, playPausePosition)
                }
                oldState == PlayerControls.STATE_PLAY && currentState == PlayerControls.STATE_PAUSE -> {
                    //isPlayPauseAnimationInfinite = false
                    getAnimatedIcon(R.drawable.ic_pause_to_play, playPausePosition)
                }
                oldState == PlayerControls.STATE_PAUSE && currentState == PlayerControls.STATE_BUFFER -> {
                    //isPlayPauseAnimationInfinite = true
                    getAnimatedIcon(R.drawable.ic_play_to_buffer, playPausePosition)
                }
                oldState == PlayerControls.STATE_PAUSE && currentState == PlayerControls.STATE_PLAY -> {
                    //isPlayPauseAnimationInfinite = false
                    getAnimatedIcon(R.drawable.ic_play_to_pause, playPausePosition)
                }
                else -> {
                    //isPlayPauseAnimationInfinite = true
                    getAnimatedIcon(R.drawable.ic_buffering, playPausePosition)
                }
            }
        } else {
            if (isFirstTime) {
                isFirstTime = false
                getAnimatedIcon(R.drawable.ic_buffer_to_play, playPausePosition)
            } else {
                playPauseIcon
            }
        }
    }
}