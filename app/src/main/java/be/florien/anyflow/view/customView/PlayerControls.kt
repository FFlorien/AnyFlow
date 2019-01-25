package be.florien.anyflow.view.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import be.florien.anyflow.R
import kotlin.math.absoluteValue


private const val CLICKABLE_SIZE_DP = 48f
private const val VISIBLE_TEXT_SP = 12f

/**
 * Created by florien on 8/01/18.
 */
class PlayerControls
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    //todo Separate context (in different class ?): drawing values, and values that are represented. Calculator which take values to represent and return drawing values
    //todo When changing track because end of track animate the change
    /**
     * Attributes
     */
    // Variable changing due to usage
    var shouldShowBuffering: Boolean = false
    var hasPrevious: Boolean = false
        set(value) {
            field = value
            if (value) {
                previousIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            } else if (_currentDuration < progressAnimDuration) {
                previousIcon.setColorFilter(disabledColor, PorterDuff.Mode.SRC_IN)
            }
        }
    var hasNext: Boolean = false
        set(value) {
            field = value
            if (value) {
                nextIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            } else {
                nextIcon.setColorFilter(disabledColor, PorterDuff.Mode.SRC_IN)
            }
        }
    private var elapsedDurationText = ""
    private var remainingDurationText = ""

    // Variables that can be configured by XML attributes
    private var _currentDuration: Int = 0
    var currentDuration: Int
        set(value) {
            val oldValue = _currentDuration

            _currentDuration = value

            if (progressAnimDuration in oldValue..value || progressAnimDuration in value..oldValue || value < oldValue) {
                previousIcon = getPreviousIcon()
            }

            computeInformationBaseline()
            computeRightBoundOfPrevButton()
            computeLeftBoundOfNextButton()
            computeTicks()
            invalidate()
        }
        get() = _currentDuration
    var state = STATE_PAUSE
        set(value) {
            if (field != value && (!playPauseIcon.isRunning || isPlayPauseAnimationInfinite)) {
                playPauseIcon = getPlayPauseIcon(value, field)
            }
            field = value
        }

    var actionListener: OnActionListener? = null
    var totalDuration: Int = 0
        get() = if (field == 0) Int.MAX_VALUE else field
    var progressAnimDuration: Int = 10000
    var smallestButtonWidth: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICKABLE_SIZE_DP, resources.displayMetrics).toInt()

    // Paints
    private val textAndOutlineColor = Paint().apply {
        val color = ContextCompat.getColor(context, R.color.iconInApp)
        setColor(color)
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, VISIBLE_TEXT_SP, resources.displayMetrics)
        strokeWidth = 2f
    }
    private val timelineOutlineColor = Paint().apply {
        val color = ContextCompat.getColor(context, R.color.iconInApp)
        setColor(color)
        strokeWidth = 2f
    }
    private val backgroundColor = Paint().apply {
        val color = ContextCompat.getColor(context, R.color.primary)
        setColor(color)
    }
    private val previousBackgroundColor = Paint().apply {
        val color = ContextCompat.getColor(context, R.color.primaryDark)
        setColor(color)
    }
    private val nextBackgroundColor = Paint().apply {
        val color = ContextCompat.getColor(context, R.color.primaryDark)
        setColor(color)
    }
    private var iconColor = ContextCompat.getColor(context, R.color.iconInApp)
    private var disabledColor = ContextCompat.getColor(context, R.color.disabled)

    // Drawables
    private var playPauseIcon: AnimatedVectorDrawableCompat
    private var isPreviousIconPrevious = false
    private var previousIcon: AnimatedVectorDrawableCompat
    private var nextIcon: AnimatedVectorDrawableCompat
    private val playPausePosition = Rect()
    private val previousIconPosition: Rect = Rect()
    private val nextIconPosition: Rect = Rect()

    // Calculations
    private var informationBaseline: Int = 0
    private var playButtonMaxWidth: Int = 0
    private var playButtonMaxWidthOffset: Int = 0
    private var prevButtonRightBound: Int = 0
    private var nextButtonLeftBound: Int = 0
    private var prevButtonRightBoundAtAnimStart: Int = 0
    private var nextButtonLeftBoundAtAnimStart: Int = 0
    private var centerLeftX: Int = 0
    private var centerRightX: Int = 0
    private val nextTicksX: FloatArray = FloatArray(6)
    private var lastDownEventX = 0f
    private var durationOnScroll = -1
    private var currentScrollOffset = 0

    private var isPlayPauseAnimationInfinite = false

    private val currentScrollDuration
        get() = (currentScrollOffset.toFloat() / (playButtonMaxWidthOffset.toFloat() / 2)) * 5000
    private val durationToDisplay
        get() = if (durationOnScroll == -1) currentDuration else durationOnScroll - currentScrollDuration.toInt()

    /**
     * Constructor
     */
    init {
        if (attrs != null) {
            val typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PlayerControls)
            currentDuration = typedArray.getInt(R.styleable.PlayerControls_currentDuration, currentDuration)
            totalDuration = typedArray.getInt(R.styleable.PlayerControls_totalDuration, totalDuration)
            progressAnimDuration = typedArray.getInt(R.styleable.PlayerControls_progressAnimDuration, progressAnimDuration)
            smallestButtonWidth = typedArray.getDimensionPixelSize(R.styleable.PlayerControls_smallestButtonWidth, smallestButtonWidth)
            state = typedArray.getInteger(R.styleable.PlayerControls_state, STATE_PAUSE)
            typedArray.getColor(R.styleable.PlayerControls_iconColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                iconColor = it
            }
            typedArray.getColor(R.styleable.PlayerControls_outLineColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                textAndOutlineColor.color = it
                timelineOutlineColor.color = it
            }
            typedArray.getColor(R.styleable.PlayerControls_previousBackgroundColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                previousBackgroundColor.color = it
            }
            typedArray.getColor(R.styleable.PlayerControls_nextBackgroundColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                nextBackgroundColor.color = it
            }
            typedArray.getColor(R.styleable.PlayerControls_progressBackgroundColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                backgroundColor.color = it
            }
            typedArray.getColor(R.styleable.PlayerControls_disabledColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                disabledColor = it
            }
            typedArray.recycle()
        }
        playPauseIcon = getPlayPauseIcon(state)
        previousIcon = getPreviousIcon()
        nextIcon = getAnimatedIcon(R.drawable.ic_next, nextIconPosition)
        if (!hasNext) {
            nextIcon.setColorFilter(disabledColor, PorterDuff.Mode.SRC_IN)
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
        val margin = smallestButtonWidth / 4
        val iconSize = smallestButtonWidth / 2
        val startX = (((desiredWidth - smallestButtonWidth) / 2) + margin)
        playPausePosition.left = startX
        playPausePosition.top = margin
        playPausePosition.right = (startX + iconSize)
        playPausePosition.bottom = (margin + iconSize)
        playPauseIcon.bounds = playPausePosition
        previousIconPosition.left = margin
        previousIconPosition.top = margin
        previousIconPosition.right = (margin + iconSize)
        previousIconPosition.bottom = (margin + iconSize)
        previousIcon.bounds = previousIconPosition
        val nextStartX = (desiredWidth - smallestButtonWidth + margin)
        nextIconPosition.left = nextStartX
        nextIconPosition.top = margin
        nextIconPosition.right = (nextStartX + iconSize)
        nextIconPosition.bottom = margin + iconSize
        nextIcon.bounds = nextIconPosition

    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, prevButtonRightBound.toFloat(), height.toFloat(), previousBackgroundColor)
        canvas.drawRect(prevButtonRightBound.toFloat(), 0f, nextButtonLeftBound.toFloat(), height.toFloat(), backgroundColor)
        canvas.drawRect(nextButtonLeftBound.toFloat(), 0f, width.toFloat(), height.toFloat(), nextBackgroundColor)
        canvas.drawLine(prevButtonRightBound.toFloat(), informationBaseline.toFloat(), nextButtonLeftBound.toFloat(), informationBaseline.toFloat(), timelineOutlineColor)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, textAndOutlineColor)
        playPauseIcon.draw(canvas)
        previousIcon.draw(canvas)
        nextIcon.draw(canvas)
        nextTicksX.filter { it > 0 }.forEach { canvas.drawLine(it, informationBaseline.toFloat(), it, (height / 4 * 3).toFloat(), timelineOutlineColor) }
        canvas.drawText(elapsedDurationText, (smallestButtonWidth / 2).toFloat(), informationBaseline.toFloat(), textAndOutlineColor)
        canvas.drawText(remainingDurationText, (width - (smallestButtonWidth / 2)).toFloat(), informationBaseline.toFloat(), textAndOutlineColor)
        canvas.drawLine(prevButtonRightBound.toFloat(), 0f, prevButtonRightBound.toFloat(), height.toFloat(), textAndOutlineColor)
        canvas.drawLine(nextButtonLeftBound.toFloat(), 0f, nextButtonLeftBound.toFloat(), height.toFloat(), textAndOutlineColor)
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
                    actionListener?.onCurrentDurationChanged((durationOnScroll - durationOffset).toLong())
                    currentScrollOffset = 0
                } else if (lastDownEventX in 0..prevButtonRightBound && event.x in 0..prevButtonRightBound) {
                    actionListener?.onPreviousClicked()
                    prevButtonRightBoundAtAnimStart = prevButtonRightBound
                    nextButtonLeftBoundAtAnimStart = nextButtonLeftBound
                } else if (lastDownEventX in nextButtonLeftBound..width && event.x in nextButtonLeftBound..width) {
                    actionListener?.onNextClicked()
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
        informationBaseline = height - 10
    }

    private fun computeRightBoundOfPrevButton() {
        prevButtonRightBound = if (durationToDisplay < progressAnimDuration) {
            getProgressLeftPosition()
        } else {
            smallestButtonWidth
        }

        val playBackTimeInSeconds = durationToDisplay / 1000
        val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
        val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
        elapsedDurationText = "$minutesDisplay:$secondsDisplay"
    }

    private fun getProgressLeftPosition(): Int {
        val percentageOfStartProgress = currentDuration.toFloat() / progressAnimDuration.toFloat()
        val offsetOfPlayButton = (percentageOfStartProgress * playButtonMaxWidthOffset).toInt()
        val maybeRightBound = smallestButtonWidth + playButtonMaxWidthOffset - offsetOfPlayButton + currentScrollOffset
        return if (maybeRightBound > smallestButtonWidth) {
            if (maybeRightBound < centerLeftX) {
                maybeRightBound
            } else {
                centerLeftX
            }
        } else {
            smallestButtonWidth
        }
    }

    private fun computeLeftBoundOfNextButton() {
        val mostRightNextLeft = width - smallestButtonWidth
        nextButtonLeftBound = if (currentDuration > totalDuration - progressAnimDuration) {
            getProgressRightPosition(mostRightNextLeft)
        } else {
            mostRightNextLeft
        }

        val playBackTimeInSeconds = (totalDuration - durationToDisplay) / 1000
        val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
        val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
        remainingDurationText = if (totalDuration == Int.MAX_VALUE) context.getString(R.string.player_controls_unknown) else "-$minutesDisplay:$secondsDisplay"
    }

    private fun getProgressRightPosition(mostRightNextLeft: Int): Int {
        val percentageOfEndProgress = (totalDuration - currentDuration).toFloat() / progressAnimDuration.toFloat()
        val offsetOfPlayButton = (percentageOfEndProgress * playButtonMaxWidthOffset).toInt()
        val maybeLeftBound = mostRightNextLeft - playButtonMaxWidthOffset + offsetOfPlayButton + currentScrollOffset
        return if (maybeLeftBound < mostRightNextLeft) {
            if (maybeLeftBound > centerRightX) {
                maybeLeftBound
            } else {
                centerRightX
            }
        } else {
            mostRightNextLeft
        }
    }

    private fun getPlayPauseIcon(newValue: Int, oldValue: Int = newValue): AnimatedVectorDrawableCompat {
        val resource = if (oldValue == STATE_BUFFER && newValue == STATE_PLAY) {
            isPlayPauseAnimationInfinite = false
            R.drawable.ic_buffer_to_pause
        } else if (oldValue == STATE_BUFFER && newValue == STATE_PAUSE) {
            isPlayPauseAnimationInfinite = false
            R.drawable.ic_buffer_to_play
        } else if (oldValue == STATE_PLAY && newValue == STATE_BUFFER) {
            isPlayPauseAnimationInfinite = true
            R.drawable.ic_pause_to_buffer
        } else if (oldValue == STATE_PLAY && newValue == STATE_PAUSE) {
            isPlayPauseAnimationInfinite = false
            R.drawable.ic_pause_to_play
        } else if (oldValue == STATE_PAUSE && newValue == STATE_BUFFER) {
            isPlayPauseAnimationInfinite = true
            R.drawable.ic_play_to_buffer
        } else if (oldValue == STATE_PAUSE && newValue == STATE_PLAY) {
            isPlayPauseAnimationInfinite = false
            R.drawable.ic_play_to_pause
        } else {
            isPlayPauseAnimationInfinite = true
            R.drawable.ic_buffering
        }

        return getAnimatedIcon(resource, playPausePosition)
    }

    private fun getPreviousIcon(): AnimatedVectorDrawableCompat {
        return if (_currentDuration > progressAnimDuration && isPreviousIconPrevious) {
            isPreviousIconPrevious = false
            getAnimatedIcon(R.drawable.ic_previous_to_start, previousIconPosition)
        } else if (!isPreviousIconPrevious) {
            isPreviousIconPrevious = true
            getAnimatedIcon(R.drawable.ic_start_to_previous, previousIconPosition).apply {
                if (!hasPrevious) {
                    setColorFilter(disabledColor, PorterDuff.Mode.SRC_IN)
                }
            }
        } else {
            previousIcon
        }
    }

    private fun getAnimatedIcon(animIconRes: Int, bounds: Rect): AnimatedVectorDrawableCompat {
        val icon = AnimatedVectorDrawableCompat.create(context, animIconRes)
                ?: throw IllegalArgumentException("Icon wasn't found !")
        icon.bounds = bounds
        icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        icon.start()
        return icon
    }

    private fun computeTicks() {
        val tickOffset = playButtonMaxWidthOffset.toFloat() / 2
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
        fun onCurrentDurationChanged(newDuration: Long)
    }

    companion object {
        const val STATE_PLAY = 0
        const val STATE_PAUSE = 1
        const val STATE_BUFFER = 2
        private const val NO_VALUE = -15
    }
}