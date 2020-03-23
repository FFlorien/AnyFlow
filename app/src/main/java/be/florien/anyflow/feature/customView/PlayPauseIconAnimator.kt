package be.florien.anyflow.feature.customView

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import be.florien.anyflow.R


class PlayPauseIconAnimator(context: Context) : IconAnimator(context) {

    override fun getStartAnimation(newState: Int): AnimatedVectorDrawableCompat? = when (oldState) {
        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE -> getAnimatedIcon(R.drawable.ic_play_end)
        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY -> getAnimatedIcon(R.drawable.ic_pause_end)
        PlayPauseIconAnimator.STATE_PLAY_PAUSE_SCROLL -> null
        else -> null
    }

    override fun getEndAnimation(newState: Int): AnimatedVectorDrawableCompat? = when (newState) {
        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE -> getAnimatedIcon(R.drawable.ic_play_start)
        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY -> getAnimatedIcon(R.drawable.ic_pause_start)
        PlayPauseIconAnimator.STATE_PLAY_PAUSE_SCROLL -> null
        else -> null
    }

    override fun getFixedIcon(newState: Int): Drawable = when (newState) {
        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE -> getIcon(R.drawable.ic_play)
        PlayPauseIconAnimator.STATE_PLAY_PAUSE_PLAY -> getIcon(R.drawable.ic_pause)
        PlayPauseIconAnimator.STATE_PLAY_PAUSE_SCROLL -> getAnimatedIcon(R.drawable.ic_scrolling)
        else -> getAnimatedIcon(R.drawable.ic_buffering)
    }

    companion object {
        const val STATE_PLAY_PAUSE_PLAY = 0
        const val STATE_PLAY_PAUSE_PAUSE = 1
        const val STATE_PLAY_PAUSE_BUFFER = 2
        const val STATE_PLAY_PAUSE_SCROLL = 3
    }
}