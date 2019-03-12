package be.florien.anyflow.view.customView

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.util.TypedValue


abstract class PlayerPainter(val context: Context) {
    var duration = 0
        protected set(value) {
            val oldValue = field
            field = value
            if (progressAnimDuration in oldValue..value || progressAnimDuration in value..oldValue || value < oldValue) {
                computePreviousIcon()
            }
            computeElapsedDurationText()
            computeRemainingDurationText()

            computePlayButtonLeftBound()
            computePlayButtonRightBound()
            computeTicks()
            onValuesComputed()
        }
    var totalDuration: Int = 0
    var currentState: Int = PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
        set(value) {
            oldState = field
            field = value
            computePlayPauseIcon()
            onValuesComputed()
        }
    var smallestButtonWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICKABLE_SIZE_DP, context.resources.displayMetrics).toInt()
    var onValuesComputed: () -> Unit = {}

    protected var oldState: Int = PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
    protected var shouldShowBuffering: Boolean = false
    protected var elapsedDurationText: String = ""
    protected var remainingDurationText: String = ""
    protected var playButtonMaxWidthOffset = 0
    protected val progressAnimDuration: Int = 10000
    protected var playButtonLeftBound: Int = 0
    protected var playButtonRightBound: Int = 0
    protected val ticks: FloatArray = FloatArray(6)

    abstract fun retrieveLayoutProperties(values: TypedArray)
    abstract fun measure(width: Int, height: Int)
    abstract fun draw(canvas: Canvas, width: Int, height: Int)
    abstract fun getButtonClicked(lastDownEventX: Int, downEventX: Int): Int
    protected abstract fun computeElapsedDurationText()
    protected abstract fun computeRemainingDurationText()
    protected abstract fun computePlayButtonLeftBound()
    protected abstract fun computePlayButtonRightBound()
    protected abstract fun computeTicks()
    abstract fun computePreviousIcon()
    protected abstract fun computePlayPauseIcon()
    abstract var hasPrevious: Boolean
    abstract var hasNext: Boolean

    companion object {
        const val CLICK_PREVIOUS = 0
        const val CLICK_PLAY_PAUSE = 1
        const val CLICK_NEXT = 2
        const val CLICK_SCROLL = 3
        const val CLICK_UNKNOWN = -1
        private const val CLICKABLE_SIZE_DP = 48f
    }
}