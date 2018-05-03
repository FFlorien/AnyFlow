package be.florien.ampacheplayer.view.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import be.florien.ampacheplayer.R
import timber.log.Timber
import kotlin.math.absoluteValue


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
    private var elapsedDurationText = ""
    private var remainingDurationText = ""

    // Variables that can be configured by XML attributes
    var currentDuration: Int = 0
        set(value) {
            field = value
            computeRightBoundOfPrevButton()
            computeLeftBoundOfNextButton()
            computeTicks()
            invalidate()
        }
    var actionListener: OnActionListener? = null

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

    private val textScrollingPaint = Paint().apply {
        setARGB(255, 5, 5, 65)
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
    private val playIconPath: Path by lazy {
        Path().apply {
            val margin = height / 4
            val iconSize = height / 2
            val startX = (((width - smallestButtonWidth) / 2) + margin).toFloat()
            val startY = margin.toFloat()
            moveTo(startX, startY)
            lineTo(startX + iconSize, startY + (iconSize / 2))
            lineTo(startX, startY + iconSize)
            lineTo(startX, startY)
        }
    }
    private val nextIconPath: Path by lazy {
        Path().apply {
            val margin = height / 4
            val iconHeight = height / 2
            val arrowsWidth = iconHeight - (iconHeight / 10)
            val startX = (width - smallestButtonWidth + margin).toFloat()
            val startY = margin.toFloat()
            val midIconHeight = iconHeight / 2
            val midArrowsWidth = arrowsWidth / 2
            val verticalCenter = startY + midIconHeight
            val horizontalCenter = startX + midArrowsWidth
            moveTo(startX, startY)
            lineTo(horizontalCenter, verticalCenter)
            lineTo(horizontalCenter, startY)
            lineTo(startX + arrowsWidth, verticalCenter)
            lineTo(horizontalCenter, startY + iconHeight)
            lineTo(horizontalCenter, verticalCenter)
            lineTo(startX, startY + iconHeight)
            lineTo(startX, startY)
            addRect(startX + arrowsWidth, startY, startX + (height / 2).toFloat(), startY + iconHeight, Path.Direction.CW)
        }
    }
    private val prevIconPath: Path by lazy {
        Path().apply {
            val margin = height / 4
            val iconHeight = height / 2
            val arrowsWidth = iconHeight - (iconHeight / 10)
            val startX = margin.toFloat() + iconHeight
            val startY = margin.toFloat()
            val midIconHeight = iconHeight / 2
            val midArrowsWidth = (arrowsWidth / 2)
            val centerY = startY + midIconHeight
            val centerX = startX - midArrowsWidth
            moveTo(startX, startY)
            lineTo(centerX, centerY)
            lineTo(centerX, startY)
            lineTo(startX - arrowsWidth, centerY)
            lineTo(centerX, startY + iconHeight)
            lineTo(centerX, centerY)
            lineTo(startX, startY + iconHeight)
            lineTo(startX, startY)
            addRect(margin.toFloat(), startY, startX - arrowsWidth, startY + iconHeight, Path.Direction.CW)
        }
    }
    private var playButtonMaxWidth: Int = 0
    private var playButtonMaxWidthOffset: Int = 0
    private var prevButtonRightBound: Int = 0
    private var nextButtonLeftBound: Int = 0
    private var centerLeftX: Int = 0
    private var centerRightX: Int = 0
    private val nextTicksX: FloatArray = FloatArray(6)

    // Variables used for gestures
    private var lastDownEventX = 0f
    private var durationOnScroll = -1
    private var currentScrollOffset = 0


    init {
        Timber.tag(this.javaClass.simpleName)
        if (attrs != null) {
            val typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PlayerControls)
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
        centerLeftX = ((desiredWidth - smallestButtonWidth).toFloat() / 2).toInt()
        centerRightX = centerLeftX + smallestButtonWidth
        playButtonMaxWidthOffset = centerLeftX - smallestButtonWidth
        playButtonMaxWidth = desiredWidth - (smallestButtonWidth * 2)
    }

    override fun onDraw(canvas: Canvas) {
        val baseline = height - 10f
        canvas.drawLine(prevButtonRightBound.toFloat(), baseline, nextButtonLeftBound.toFloat(), baseline, timelinePaint)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, timelinePaint)
        canvas.drawPath(playIconPath, buttonPaint)
        nextTicksX.filter { it > 0 }.forEach { canvas.drawLine(it, baseline, it, (height / 4 * 3).toFloat(), timelinePaint) }
        val selectedTextPaint = if (durationOnScroll == -1) textPaint else textScrollingPaint
        canvas.drawText(elapsedDurationText, (smallestButtonWidth / 2).toFloat(), baseline, selectedTextPaint)
        canvas.drawText(remainingDurationText, (width - (smallestButtonWidth / 2)).toFloat(), baseline, selectedTextPaint)
        canvas.drawLine(prevButtonRightBound.toFloat(), 0f, prevButtonRightBound.toFloat(), height.toFloat(), timelinePaint)
        canvas.drawPath(prevIconPath, buttonPaint)
        canvas.drawLine(nextButtonLeftBound.toFloat(), 0f, nextButtonLeftBound.toFloat(), height.toFloat(), timelinePaint)
        canvas.drawPath(nextIconPath, buttonPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastDownEventX = event.x
                durationOnScroll = currentDuration
            }
            MotionEvent.ACTION_UP -> {
                if (currentScrollOffset.absoluteValue > smallestButtonWidth.absoluteValue) {
                    val durationOffset = (currentScrollOffset.toFloat() / (playButtonMaxWidthOffset.toFloat() / 2)) * 5000
                    actionListener?.onCurrentDurationChanged((durationOnScroll - durationOffset).toInt())
                    currentScrollOffset = 0
                } else if (lastDownEventX in 0..prevButtonRightBound && event.x in 0..prevButtonRightBound) {
                    actionListener?.onPreviousClicked()
                } else if (lastDownEventX in nextButtonLeftBound..width && event.x in nextButtonLeftBound..width) {
                    actionListener?.onNextClicked()
                } else if (lastDownEventX in prevButtonRightBound..nextButtonLeftBound && event.x in prevButtonRightBound..nextButtonLeftBound) {
                    actionListener?.onPlayPauseClicked()
                } else {
                    return super.onTouchEvent(event)
                }
                lastDownEventX = 0f
                durationOnScroll = -1
            }
            MotionEvent.ACTION_MOVE -> {
                currentScrollOffset = (event.x - lastDownEventX).toInt()
            }
            else -> return super.onTouchEvent(event)
        }
        return true
    }

    /**
     * Private methods
     */

    private fun computeRightBoundOfPrevButton() {
        val currentScrollDuration = (currentScrollOffset.toFloat() / (playButtonMaxWidthOffset.toFloat() / 2)) * 5000
        prevButtonRightBound = if (currentDuration - currentScrollDuration < progressAnimDuration) {
            val percentageOfStartProgress = currentDuration.toFloat() / progressAnimDuration.toFloat()
            val offsetOfPlayButton = (percentageOfStartProgress * playButtonMaxWidthOffset).toInt()
            val maybeRightBound = smallestButtonWidth + playButtonMaxWidthOffset - offsetOfPlayButton + currentScrollOffset
            if (maybeRightBound > smallestButtonWidth) maybeRightBound else smallestButtonWidth
        } else {
            smallestButtonWidth
        }

        val durationToDisplay = if (durationOnScroll == -1) currentDuration else durationOnScroll - currentScrollDuration.toInt()
        val playBackTimeInSeconds = durationToDisplay / 1000
        val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
        val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
        elapsedDurationText = "$minutesDisplay:$secondsDisplay"
    }

    private fun computeLeftBoundOfNextButton() {
        val currentScrollDuration = (currentScrollOffset.toFloat() / (playButtonMaxWidthOffset.toFloat() / 2)) * 5000
        val mostRightNextLeft = width - smallestButtonWidth

        nextButtonLeftBound = if (currentDuration > totalDuration - progressAnimDuration) {
            val percentageOfEndProgress = (totalDuration - currentDuration).toFloat() / progressAnimDuration.toFloat()
            val offsetOfPlayButton = (percentageOfEndProgress * playButtonMaxWidthOffset).toInt()
            val maybeLeftBound = mostRightNextLeft - playButtonMaxWidthOffset + offsetOfPlayButton + currentScrollOffset
            if (maybeLeftBound < mostRightNextLeft) maybeLeftBound else mostRightNextLeft
        } else {
            mostRightNextLeft
        }

        val durationToDisplay = if (durationOnScroll == -1) currentDuration else durationOnScroll - currentScrollDuration.toInt()
        val playBackTimeInSeconds = (totalDuration - durationToDisplay) / 1000
        val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
        val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
        remainingDurationText = "-$minutesDisplay:$secondsDisplay"
    }

    private fun computeTicks() {
        val currentScrollDuration = (currentScrollOffset.toFloat() / (playButtonMaxWidthOffset.toFloat() / 2)) * 5000
        val tickOffset = playButtonMaxWidthOffset.toFloat() / 2
        val durationToDisplay = if (durationOnScroll == -1) currentDuration else durationOnScroll - currentScrollDuration.toInt()
        val nextTickInDuration = 5000 - (durationToDisplay % 5000)
        val percentageOfDuration = nextTickInDuration.toFloat() / 10000f
        val firstTickX = (width / 2) + (percentageOfDuration * playButtonMaxWidthOffset) - playButtonMaxWidthOffset - (smallestButtonWidth / 2)

        for (i in 0 until 6) {
            val maybeTickX = firstTickX + (tickOffset * i)
            nextTicksX[i] = when (maybeTickX) {
                in prevButtonRightBound..nextButtonLeftBound -> maybeTickX
                else -> -1f
            }
        }
    }

    /**
     * Listeners
     */
    interface OnActionListener {
        fun onPreviousClicked()
        fun onNextClicked()
        fun onPlayPauseClicked()
        fun onCurrentDurationChanged(newDuration: Int)
    }
}