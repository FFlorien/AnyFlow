package be.florien.ampacheplayer.view.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import be.florien.ampacheplayer.R


private const val CLICKABLE_SIZE_DP = 48f
private const val VISIBLE_TEXT_SP = 12f

/**
 * Created by florien on 8/01/18.
 */
class PlayerControls
@JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    /**
     * Attributes
     */
    // Variable changing due to usage
    var hasPrevious: Boolean = false
    var hasNext: Boolean = false

    // Variables that can be configured by XML attributes
    var durationText: String = "00:00"
    var currentDuration: Int = 0
        set(value) {
            field = value
            computeRightBoundOfPrevButton()
            computeLeftBoundOfNextButton()
            computeTicks()
            invalidate()
        }

    var totalDuration: Int = 0
    var progressAnimDuration: Int = 10000
    var changeTrackAnimDuration: Int = 0
    var smallestButtonWidth: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICKABLE_SIZE_DP, resources.displayMetrics).toInt()

    // Variable used for drawing
    private val textPaint = Paint().apply {
        setARGB(255, 65, 65, 65)
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, VISIBLE_TEXT_SP, resources.displayMetrics)
        strokeWidth = 2f
    }
    private val timelinePaint = Paint().apply {
        setARGB(255, 65, 65, 65)
        strokeWidth = 2f
    }
    private val buttonPaint = Paint().apply {
        setARGB(255, 45, 25, 0)
        strokeWidth = 2f
    }
    private var playButtonMaxWidthOffset: Int = 0
    private var prevButtonRightBound: Int = 0
    private var nextButtonLeftBound: Int = 0
    private var centerLeftX: Int = 0
    private var centerRightX: Int = 0
    private val nextTicksX: FloatArray = FloatArray(6)

    // Variables used for gestures
    private var lastDownEventX = 0f
    var onPreviousClicked: OnPreviousClickedListener? = null
    var onNextClicked: OnNextClickedListener? = null
    var onPlayPauseClicked: OnPlayPauseClickedListener? = null


    init {
        if (attrs != null) {
            val typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PlayerControls)
            durationText = typedArray.getNonResourceString(R.styleable.PlayerControls_durationText) ?: durationText
            currentDuration = typedArray.getInt(R.styleable.PlayerControls_currentDuration, currentDuration)
            totalDuration = typedArray.getInt(R.styleable.PlayerControls_totalDuration, totalDuration)
            progressAnimDuration = typedArray.getInt(R.styleable.PlayerControls_progressAnimDuration, progressAnimDuration)
            changeTrackAnimDuration = typedArray.getInt(R.styleable.PlayerControls_changeTrackAnimDuration, changeTrackAnimDuration)
            smallestButtonWidth = typedArray.getDimensionPixelSize(R.styleable.PlayerControls_smallestButtonWidth, smallestButtonWidth)
            typedArray.recycle()
        }
    }


    /**
     * Overridden methods
     */

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        val idealButtonSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICKABLE_SIZE_DP, resources.displayMetrics).toInt()

        val desiredWidth = when (widthMode) {
            View.MeasureSpec.AT_MOST -> widthSize
            View.MeasureSpec.EXACTLY -> widthSize
            View.MeasureSpec.UNSPECIFIED -> idealButtonSize
            else -> idealButtonSize
        }

        val desiredHeight = when (heightMode) {
            View.MeasureSpec.AT_MOST -> Math.min(heightSize, idealButtonSize)
            View.MeasureSpec.EXACTLY -> heightSize
            View.MeasureSpec.UNSPECIFIED -> idealButtonSize
            else -> idealButtonSize
        }

        setMeasuredDimension(desiredWidth, desiredHeight)

        if (desiredWidth < (smallestButtonWidth * 3)) {
            smallestButtonWidth = desiredWidth / 3
        }
        centerLeftX = ((desiredWidth - smallestButtonWidth).toFloat() /2).toInt()
        centerRightX = centerLeftX + smallestButtonWidth
        playButtonMaxWidthOffset = centerLeftX - smallestButtonWidth
    }

    override fun onDraw(canvas: Canvas) {
        val playButtonLeftBound = (smallestButtonWidth + playButtonMaxWidthOffset).toFloat()
        val playButtonRightBound = (width - smallestButtonWidth - playButtonMaxWidthOffset).toFloat()
        val baseline = height - 10f
        canvas.drawLine(prevButtonRightBound.toFloat(), baseline, playButtonLeftBound, baseline, timelinePaint)
        canvas.drawLine(playButtonRightBound, baseline, (width - smallestButtonWidth).toFloat(), baseline, timelinePaint)
        canvas.drawLine(centerLeftX.toFloat(), 0f, centerLeftX.toFloat(), height.toFloat(), textPaint)
        canvas.drawLine(centerRightX.toFloat(), 0f, centerRightX.toFloat(), height.toFloat(), textPaint)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, timelinePaint)
        nextTicksX.filter { it > 0 }.forEach { canvas.drawLine(it, baseline, it, baseline - 20, timelinePaint) }
        canvas.drawText(durationText, playButtonLeftBound + (smallestButtonWidth / 2), baseline, textPaint)
        canvas.drawRect(0f, 0f, prevButtonRightBound.toFloat(), height.toFloat(), buttonPaint)
        canvas.drawRect(nextButtonLeftBound.toFloat(), 0f, width.toFloat(), height.toFloat(), buttonPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> lastDownEventX = event.x
            MotionEvent.ACTION_UP -> {
                if (lastDownEventX in 0..prevButtonRightBound && event.x in 0..prevButtonRightBound) {
                    onPreviousClicked?.onPreviousClicked()
                } else if (lastDownEventX in nextButtonLeftBound..width && event.x in nextButtonLeftBound..width) {
                    onNextClicked?.onNextClicked()
                } else if (lastDownEventX in prevButtonRightBound..nextButtonLeftBound && event.x in prevButtonRightBound..nextButtonLeftBound) {
                    onPlayPauseClicked?.onPlayPauseClicked()
                } else {
                    return super.onTouchEvent(event)
                }
            }
            else -> return super.onTouchEvent(event)
        }
        return true
    }

    /**
     * Private methods
     */

    private fun computeRightBoundOfPrevButton() {
        prevButtonRightBound = if (currentDuration < progressAnimDuration) {
            val percentageOfStartProgress = currentDuration.toFloat() / progressAnimDuration.toFloat()
            val offsetOfPlayButton = (percentageOfStartProgress * playButtonMaxWidthOffset).toInt()
            smallestButtonWidth + playButtonMaxWidthOffset - offsetOfPlayButton
        } else {
            smallestButtonWidth
        }
    }

    private fun computeLeftBoundOfNextButton() {
        val mostRightNextLeft = width - smallestButtonWidth

        nextButtonLeftBound = if (currentDuration > totalDuration - progressAnimDuration) {
            val percentageOfEndProgress = (totalDuration - currentDuration).toFloat() / progressAnimDuration.toFloat()
            val offsetOfPlayButton = (percentageOfEndProgress * playButtonMaxWidthOffset).toInt()
            mostRightNextLeft - playButtonMaxWidthOffset + offsetOfPlayButton
        } else {
            mostRightNextLeft
        }
    }

    private fun computeTicks() {
        val tickOffset = playButtonMaxWidthOffset.toFloat() / 2
        val nextTickInDuration = 5000 - ((currentDuration - 5000) % 5000)
        val percentageOfDuration = nextTickInDuration.toFloat() / 10000f
        val firstTickX = smallestButtonWidth + (percentageOfDuration * playButtonMaxWidthOffset)

        for (i in 0 until 6) {
            val maybeTickX = firstTickX + (tickOffset * i)
            nextTicksX[i] = when {
                maybeTickX in prevButtonRightBound..centerLeftX -> maybeTickX
                (maybeTickX + smallestButtonWidth) in centerRightX..nextButtonLeftBound -> maybeTickX + smallestButtonWidth
                else -> -1f
            }
        }
    }

    /**
     * Listeners
     */
    interface OnPreviousClickedListener {
        fun onPreviousClicked()
    }

    interface OnNextClickedListener {
        fun onNextClicked()
    }

    interface OnPlayPauseClickedListener {
        fun onPlayPauseClicked()
    }
}