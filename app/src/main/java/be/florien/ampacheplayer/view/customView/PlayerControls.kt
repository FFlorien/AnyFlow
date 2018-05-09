package be.florien.ampacheplayer.view.customView

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
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

    //todo Separate context (in different class ?): drawing values, and values that are represented. Calculator which take values to represent and return drawing values
    //todo When changing track because end of track animate the change
    /**
     * Attributes
     */
    // Variable changing due to usage
    var hasPrevious: Boolean = false
    var hasNext: Boolean = false
    private var elapsedDurationText = ""
    private var remainingDurationText = ""

    // Variables that can be configured by XML attributes
    private var _currentDuration: Int = 0
    var currentDuration: Int
        set(value) {
            if ((isAnimatingNextTrack || isAnimatingPreviousTrack) && value > changeTrackAnimDuration && value < totalDuration - 100) {
                isAnimatingPreviousTrack = false
                isAnimatingNextTrack = false
            } else if (value >= totalDuration - 100) {
                isAnimatingNextTrack = true
            }

            if (!isAnimatingNextTrack && !isAnimatingPreviousTrack) {
                _currentDuration = value
            }

            computeInformationBaseline()
            computeRightBoundOfPrevButton()
            computeLeftBoundOfNextButton()
            computeBonusButtonX()
            computeTimelineBoundaries()
            computeTicks()
            invalidate()
        }
        get() = _currentDuration

    var actionListener: OnActionListener? = null
    var totalDuration: Int = 0
    var progressAnimDuration: Int = 10000
    var changeTrackAnimDuration: Int = 0
    var smallestButtonWidth: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICKABLE_SIZE_DP, resources.displayMetrics).toInt()

    // Paints
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
    private val fixedTimelinePaint = Paint().apply {
        setARGB(255, 65, 65, 65)
        strokeWidth = 2f
    }
    private val buttonPaint = Paint().apply {
        setARGB(255, 45, 25, 0)
        strokeWidth = 2f
    }

    // Paths
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

    // Calculations
    private var informationBaseline: Int = 0
    private var playButtonMaxWidth: Int = 0
    private var playButtonMaxWidthOffset: Int = 0
    private var timelineLeftBound: Int = 0
    private var timelineRightBound: Int = 0
    private var prevButtonRightBound: Int = 0
    private var prevButtonRightBoundAtAnimStart: Int = 0
    private var nextButtonLeftBound: Int = 0
    private var nextButtonLeftBoundAtAnimStart: Int = 0
    private var bonusButtonBarX: Int = 0
    private var centerLeftX: Int = 0
    private var centerRightX: Int = 0
    private val nextTicksX: FloatArray = FloatArray(6)
    private var lastDownEventX = 0f
    private var durationOnScroll = -1
    private var currentScrollOffset = 0

    private var isAnimatingPreviousTrack = false
    private var isAnimatingNextTrack = false
    private val changeTrackAnimator: ValueAnimator

    private val currentScrollDuration
        get() = (currentScrollOffset.toFloat() / (playButtonMaxWidthOffset.toFloat() / 2)) * 5000
    private val durationToDisplay
        get() = if (durationOnScroll == -1) currentDuration else durationOnScroll - currentScrollDuration.toInt()

    /**
     * Constructor
     */
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
        changeTrackAnimator = ValueAnimator
                .ofInt(0, changeTrackAnimDuration)
                .setDuration(changeTrackAnimDuration.toLong())
                .apply {
                    interpolator = AccelerateInterpolator()
                    addUpdateListener {
                        _currentDuration = it.animatedValue as Int
                        currentDuration = it.animatedValue as Int
                    }
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
        informationBaseline = (height - 10f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawLine(timelineLeftBound.toFloat(), informationBaseline.toFloat(), timelineRightBound.toFloat(), informationBaseline.toFloat(), timelinePaint)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, fixedTimelinePaint)
        canvas.drawPath(playIconPath, buttonPaint)
        nextTicksX.filter { it > 0 }.forEach { canvas.drawLine(it, informationBaseline.toFloat(), it, (height / 4 * 3).toFloat(), timelinePaint) }
        val selectedTextPaint = if (durationOnScroll == -1) textPaint else textScrollingPaint
        canvas.drawText(elapsedDurationText, (smallestButtonWidth / 2).toFloat(), informationBaseline.toFloat(), selectedTextPaint)
        canvas.drawText(remainingDurationText, (width - (smallestButtonWidth / 2)).toFloat(), informationBaseline.toFloat(), selectedTextPaint)
        canvas.drawLine(prevButtonRightBound.toFloat(), 0f, prevButtonRightBound.toFloat(), height.toFloat(), fixedTimelinePaint)
        canvas.drawPath(prevIconPath, buttonPaint)
        canvas.drawLine(nextButtonLeftBound.toFloat(), 0f, nextButtonLeftBound.toFloat(), height.toFloat(), fixedTimelinePaint)
        canvas.drawPath(nextIconPath, buttonPaint)
        canvas.drawLine(bonusButtonBarX.toFloat(), 0f, bonusButtonBarX.toFloat(), height.toFloat(), fixedTimelinePaint)
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
                    isAnimatingPreviousTrack = true
                    changeTrackAnimator.start()
                    prevButtonRightBoundAtAnimStart = prevButtonRightBound
                    nextButtonLeftBoundAtAnimStart = nextButtonLeftBound
                } else if (lastDownEventX in nextButtonLeftBound..width && event.x in nextButtonLeftBound..width) {
                    actionListener?.onNextClicked()
                    isAnimatingNextTrack = true
                    changeTrackAnimator.start()
                    prevButtonRightBoundAtAnimStart = prevButtonRightBound
                    nextButtonLeftBoundAtAnimStart = nextButtonLeftBound
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

    private fun computeInformationBaseline() {
//        informationBaseline = if (isAnimatingPreviousTrack || isAnimatingNextTrack && durationToDisplay < changeTrackAnimDuration) {
//            val animPercentage = durationToDisplay.toFloat() / changeTrackAnimDuration.toFloat()
//            val offset = 20 * (1 - animPercentage)
//            (height - 10 + offset).toInt()
//        } else {
//            height - 10
//        }
        informationBaseline = height - 10
    }

    private fun computeRightBoundOfPrevButton() {
        prevButtonRightBound = if (isAnimatingPreviousTrack && durationToDisplay < changeTrackAnimDuration) {
            val animPercentage = durationToDisplay.toFloat() / changeTrackAnimDuration.toFloat()
            (centerLeftX.toFloat() * animPercentage).toInt()
        } else if (isAnimatingNextTrack && durationToDisplay < changeTrackAnimDuration) {
            val animPercentage = durationToDisplay.toFloat() / changeTrackAnimDuration.toFloat()
            val offset = (nextButtonLeftBoundAtAnimStart - centerLeftX) * (1 - animPercentage)
            (centerLeftX.toFloat() + offset).toInt()
        } else if (durationToDisplay < progressAnimDuration) {
            val percentageOfStartProgress = currentDuration.toFloat() / progressAnimDuration.toFloat()
            val offsetOfPlayButton = (percentageOfStartProgress * playButtonMaxWidthOffset).toInt()
            val maybeRightBound = smallestButtonWidth + playButtonMaxWidthOffset - offsetOfPlayButton + currentScrollOffset
            if (maybeRightBound > smallestButtonWidth) {
                if (maybeRightBound < centerLeftX) {
                    maybeRightBound
                } else {
                    centerLeftX
                }
            } else {
                smallestButtonWidth
            }
        } else {
            smallestButtonWidth
        }

        val playBackTimeInSeconds = durationToDisplay / 1000
        val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
        val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
        elapsedDurationText = "$minutesDisplay:$secondsDisplay"
    }

    private fun computeLeftBoundOfNextButton() {
        val mostRightNextLeft = width - smallestButtonWidth

        nextButtonLeftBound = if (isAnimatingPreviousTrack && durationToDisplay < changeTrackAnimDuration) {
            val animPercentage = durationToDisplay.toFloat() / changeTrackAnimDuration.toFloat()
            val offset = (width - smallestButtonWidth - prevButtonRightBoundAtAnimStart) * animPercentage
            (prevButtonRightBoundAtAnimStart + offset).toInt()
        } else if (isAnimatingNextTrack && durationToDisplay < changeTrackAnimDuration) {
            val animPercentage = durationToDisplay.toFloat() / changeTrackAnimDuration.toFloat()
            val offset = smallestButtonWidth * animPercentage
            (width - offset).toInt()
        } else if (currentDuration > totalDuration - progressAnimDuration) {
            val percentageOfEndProgress = (totalDuration - currentDuration).toFloat() / progressAnimDuration.toFloat()
            val offsetOfPlayButton = (percentageOfEndProgress * playButtonMaxWidthOffset).toInt()
            val maybeLeftBound = mostRightNextLeft - playButtonMaxWidthOffset + offsetOfPlayButton + currentScrollOffset
            if (maybeLeftBound < mostRightNextLeft) {
                if (maybeLeftBound > centerRightX) {
                    maybeLeftBound
                } else {
                    centerRightX
                }
            } else {
                mostRightNextLeft
            }
        } else {
            mostRightNextLeft
        }

        val playBackTimeInSeconds = (totalDuration - durationToDisplay) / 1000
        val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
        val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
        remainingDurationText = "-$minutesDisplay:$secondsDisplay"
    }

    private fun computeTimelineBoundaries() {
        val animPercentage = durationToDisplay.toFloat() / changeTrackAnimDuration.toFloat()
        timelineLeftBound = if (isAnimatingPreviousTrack && durationToDisplay < changeTrackAnimDuration) {
            if (animPercentage < 0.5) {
                nextButtonLeftBound
            } else {
                prevButtonRightBound
            }
        } else if (isAnimatingNextTrack && durationToDisplay < changeTrackAnimDuration) {
            if (animPercentage < 0.5) {
                bonusButtonBarX
            } else {
                nextButtonLeftBound
            }
        } else {
            prevButtonRightBound
        }
        timelineRightBound = if (isAnimatingPreviousTrack && durationToDisplay < changeTrackAnimDuration) {
            if (animPercentage < 0.5) {
                bonusButtonBarX
            } else {
                nextButtonLeftBound
            }
        } else if (isAnimatingNextTrack && durationToDisplay < changeTrackAnimDuration) {
            if (animPercentage < 0.5) {
                prevButtonRightBound
            } else {
                nextButtonLeftBound
            }
        } else {
            nextButtonLeftBound
        }
        timelinePaint.alpha = if ((isAnimatingPreviousTrack || isAnimatingNextTrack) && animPercentage < 0.5) {
            (255 - (255 * (animPercentage * 2))).toInt()
        } else if (isAnimatingPreviousTrack || isAnimatingNextTrack) {
            (255 * ((animPercentage - 0.5) * 2)).toInt()
        } else {
            255
        }
    }

    private fun computeBonusButtonX() {
        bonusButtonBarX = if (isAnimatingPreviousTrack && durationToDisplay < changeTrackAnimDuration) {
            val animPercentage = durationToDisplay.toFloat() / changeTrackAnimDuration.toFloat()
            val offset = (width - nextButtonLeftBoundAtAnimStart) * animPercentage
            nextButtonLeftBoundAtAnimStart + offset.toInt()
        } else if (isAnimatingNextTrack && durationToDisplay < changeTrackAnimDuration) {
            val animPercentage = durationToDisplay.toFloat() / changeTrackAnimDuration.toFloat()
            (prevButtonRightBoundAtAnimStart * (1 - animPercentage)).toInt()
        } else {
            -1
        }
    }

    private fun computeTicks() {
        val tickOffset = playButtonMaxWidthOffset.toFloat() / 2
        val nextTickInDuration = 5000 - (durationToDisplay % 5000)
        val percentageOfDuration = nextTickInDuration.toFloat() / 10000f
        val firstTickX = (width / 2) + (percentageOfDuration * playButtonMaxWidthOffset) - playButtonMaxWidthOffset - (smallestButtonWidth / 2)

        for (i in 0 until 6) {
            val maybeTickX = firstTickX + (tickOffset * i)
            nextTicksX[i] = when (maybeTickX) {
                in timelineLeftBound..timelineRightBound -> maybeTickX
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