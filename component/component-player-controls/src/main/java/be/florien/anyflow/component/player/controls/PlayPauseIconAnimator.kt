package be.florien.anyflow.component.player.controls

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat


class PlayPauseIconAnimator(context: Context) : IconAnimator(context) {

    override fun getStartAnimation(newState: Int): AnimatedVectorDrawableCompat? = when (oldState) {
        STATE_PLAY_PAUSE_PAUSE -> if (newState == STATE_PLAY_PAUSE_PLAY) {
            getAnimatedIcon(R.drawable.ic_play_to_pause)
        } else {
            getAnimatedIcon(R.drawable.ic_from_play)
        }

        STATE_PLAY_PAUSE_PLAY -> if (newState == STATE_PLAY_PAUSE_PAUSE) {
            getAnimatedIcon(R.drawable.ic_pause_to_play)
        } else {
            getAnimatedIcon(R.drawable.ic_from_pause)
        }

        STATE_PLAY_PAUSE_SCROLL -> null
        else -> null
    }

    override fun getEndAnimation(newState: Int): AnimatedVectorDrawableCompat? = when (newState) {
        STATE_PLAY_PAUSE_PAUSE -> if (oldState == STATE_PLAY_PAUSE_PLAY) {
            null
        } else {
            getAnimatedIcon(R.drawable.ic_to_play)
        }

        STATE_PLAY_PAUSE_PLAY -> if (oldState == STATE_PLAY_PAUSE_PAUSE) {
            null
        } else {
            getAnimatedIcon(R.drawable.ic_to_pause)
        }

        STATE_PLAY_PAUSE_SCROLL -> null
        else -> null
    }

    override fun getFixedIcon(newState: Int): Drawable = when (newState) {
        STATE_PLAY_PAUSE_PAUSE -> getIcon(R.drawable.ic_play)
        STATE_PLAY_PAUSE_PLAY -> getIcon(R.drawable.ic_pause)
        STATE_PLAY_PAUSE_SCROLL -> getAnimatedIcon(R.drawable.ic_scrolling)
        else -> getAnimatedIcon(R.drawable.ic_buffering)
    }

    companion object {
        const val STATE_PLAY_PAUSE_PLAY = 0
        const val STATE_PLAY_PAUSE_PAUSE = 1
        const val STATE_PLAY_PAUSE_BUFFER = 2
        const val STATE_PLAY_PAUSE_SCROLL = 3
    }
}