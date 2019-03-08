package be.florien.anyflow.view.customView

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import be.florien.anyflow.R


class PlayPauseIconAnimator(context: Context) : IconAnimator(context) {

    override fun getStartAnimation(newState: Int): AnimatedVectorDrawableCompat? {
        return when (oldState) {
            PlayerControls.STATE_PAUSE -> getAnimatedIcon(R.drawable.ic_play_end)
            PlayerControls.STATE_PLAY -> getAnimatedIcon(R.drawable.ic_pause_end)
            PlayerControls.STATE_SCROLL -> null
            else -> null
        }
    }

    override fun getEndAnimation(newState: Int): AnimatedVectorDrawableCompat? {
        return when (newState) {
            PlayerControls.STATE_PAUSE -> getAnimatedIcon(R.drawable.ic_play_start)
            PlayerControls.STATE_PLAY -> getAnimatedIcon(R.drawable.ic_pause_start)
            PlayerControls.STATE_SCROLL -> null
            else -> null
        }
    }

    override fun getFixedIcon(newState: Int): Drawable {
        return when (newState) {
            PlayerControls.STATE_PAUSE -> getIcon(R.drawable.ic_play)
            PlayerControls.STATE_PLAY -> getIcon(R.drawable.ic_pause)
            PlayerControls.STATE_SCROLL -> getAnimatedIcon(R.drawable.ic_scrolling)
            else -> getAnimatedIcon(R.drawable.ic_buffering)
        }
    }
}