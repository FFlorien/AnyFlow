package be.florien.anyflow.view.customView

import android.content.Context
import android.content.res.TypedArray
import be.florien.anyflow.R
import kotlin.math.absoluteValue


internal class ScrollPlayerPainter(context: Context) : DurationPlayerPainter(context) {

    var durationOnScrollStart: Int = 0
    var scrollOffset: Float = 0F
        set(value) {
            field = value
            duration = (durationOnScrollStart - durationOffset).toInt()
        }
    private val durationOffset
            get() = (scrollOffset/ (playButtonMaxWidthOffset / 2)) * 5000

    private var minimumDurationOffset = 50000
    override fun retrieveLayoutProperties(values: TypedArray) {
        super.retrieveLayoutProperties(values)
        minimumDurationOffset = values.getInt(R.styleable.PlayerControls_minimumDurationForSeek, minimumDurationOffset)
    }

    override fun getButtonClicked(lastDownEventX: Int, downEventX: Int): Int =
            if (durationOffset.absoluteValue > minimumDurationOffset) {
                CLICK_SCROLL
            } else {
                CLICK_UNKNOWN
            }

    override fun computePlayPauseIcon() {
        playPauseIcon = getAnimatedIcon(R.drawable.ic_buffering, playPausePosition)
    }
}