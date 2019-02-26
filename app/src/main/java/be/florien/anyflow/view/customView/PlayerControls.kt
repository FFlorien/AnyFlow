package be.florien.anyflow.view.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
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
    abstract class ValuesComputer {
        var duration = 0
            set(value) {

                if (progressAnimDuration in field..value || progressAnimDuration in value..field || value < field) {
                    computePreviousIcon()
                }
                field = value
                computeElapsedDurationText()
                computeRemainingDurationText()

                computePlayButtonLeftBound()
                computePlayButtonRightBound()
                computeTicks()
                onValuesComputed()
            }

        var onValuesComputed: () -> Unit = {}

        var totalDuration: Int = 0
        var shouldShowBuffering: Boolean = false
        var elapsedDurationText: String = ""
            protected set
        var remainingDurationText: String = ""
            protected set
        protected var oldState: Int = STATE_PAUSE
        var currentState: Int = STATE_PAUSE
            set(value) {
                oldState = field
                field = value
                computePlayPauseIcon()
            }
        val progressAnimDuration: Int = 10000
        var playButtonLeftBound: Int = 0
            protected set
        var playButtonRightBound: Int = 0
            protected set
        val ticks: FloatArray = FloatArray(6)

        // Drawables
        @DrawableRes
        var playPauseIcon: Int? = null
            protected set
        @DrawableRes
        var previousIcon: Int? = null
            protected set
        @DrawableRes
        var nextIcon: Int? = null
            protected set

        protected abstract fun computeElapsedDurationText()
        protected abstract fun computeRemainingDurationText()
        protected abstract fun computePlayButtonLeftBound()
        protected abstract fun computePlayButtonRightBound()
        protected abstract fun computeTicks()

        protected abstract fun computePreviousIcon()
        protected abstract fun computePlayPauseIcon()

    }

    private abstract class DurationValuesComputer(val context: Context) : ValuesComputer() {
        var smallestButtonWidth = 0
        var playButtonMaxWidthOffset = 0
        var centerLeftX = 0
        var centerRightX = 0

        private var isPreviousIconPrevious = false

        override fun computeElapsedDurationText() {
            val playBackTimeInSeconds = duration / 1000
            val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
            val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
            elapsedDurationText = "$minutesDisplay:$secondsDisplay"
        }

        override fun computeRemainingDurationText() {
            val playBackTimeInSeconds = (totalDuration - duration) / 1000
            val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
            val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
            remainingDurationText = if (totalDuration == Int.MAX_VALUE) context.getString(R.string.player_controls_unknown) else "-$minutesDisplay:$secondsDisplay"
        }

        override fun computePreviousIcon() {
            return if (duration > progressAnimDuration && isPreviousIconPrevious) {
                isPreviousIconPrevious = false
                previousIcon = R.drawable.ic_previous_to_start
            } else if (!isPreviousIconPrevious) {
                isPreviousIconPrevious = true
                previousIcon = R.drawable.ic_start_to_previous
            } else {
                previousIcon = null
            }
        }

        override fun computePlayButtonLeftBound() {
            playButtonLeftBound = if (duration < progressAnimDuration) {
                val percentageOfStartProgress = duration.toFloat() / progressAnimDuration.toFloat()
                val offsetOfPlayButton = (percentageOfStartProgress * playButtonMaxWidthOffset).toInt()
                val maybeRightBound = smallestButtonWidth + playButtonMaxWidthOffset - offsetOfPlayButton
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
        }

        override fun computePlayButtonRightBound() {
            val mostRightNextLeft = - smallestButtonWidth
            playButtonRightBound = if (duration > totalDuration - progressAnimDuration) {
                val percentageOfEndProgress = (totalDuration - duration).toFloat() / progressAnimDuration.toFloat()
                val offsetOfPlayButton = (percentageOfEndProgress * playButtonMaxWidthOffset).toInt()
                val maybeLeftBound = mostRightNextLeft - playButtonMaxWidthOffset + offsetOfPlayButton
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
        }

        override fun computeTicks() {
            val tickOffset = playButtonMaxWidthOffset.toFloat() / 2
            val nextTickInDuration = 5000 - (duration % 5000)
            val percentageOfDuration = nextTickInDuration.toFloat() / 10000f
            val firstTickX = (centerLeftX + (smallestButtonWidth /2)) + (percentageOfDuration * playButtonMaxWidthOffset) - playButtonMaxWidthOffset - (smallestButtonWidth / 2)

            for (i in 0 until 6) {
                val maybeTickX = firstTickX + (tickOffset * i)
                ticks[i] = when (maybeTickX.toInt()) {
                    in playButtonLeftBound..playButtonRightBound -> maybeTickX
                    else -> -1f
                }
            }
        }
    }


    private class PlayValuesComputer(context: Context) : DurationValuesComputer(context) {
        override fun computePlayPauseIcon() {
            playPauseIcon = if (currentState != oldState) {
                when {
                    oldState == STATE_BUFFER && currentState == STATE_PLAY -> {
                        //isPlayPauseAnimationInfinite = false
                        R.drawable.ic_buffer_to_pause
                    }
                    oldState == STATE_BUFFER && currentState == STATE_PAUSE -> {
                        //isPlayPauseAnimationInfinite = false
                        R.drawable.ic_buffer_to_play
                    }
                    oldState == STATE_PLAY && currentState == STATE_BUFFER -> {
                        //isPlayPauseAnimationInfinite = true
                        R.drawable.ic_pause_to_buffer
                    }
                    oldState == STATE_PLAY && currentState == STATE_PAUSE -> {
                        //isPlayPauseAnimationInfinite = false
                        R.drawable.ic_pause_to_play
                    }
                    oldState == STATE_PAUSE && currentState == STATE_BUFFER -> {
                        //isPlayPauseAnimationInfinite = true
                        R.drawable.ic_play_to_buffer
                    }
                    oldState == STATE_PAUSE && currentState == STATE_PLAY -> {
                        //isPlayPauseAnimationInfinite = false
                        R.drawable.ic_play_to_pause
                    }
                    else -> {
                        //isPlayPauseAnimationInfinite = true
                        R.drawable.ic_buffering
                    }
                }
            } else {
                null
            }
        }
    }

    private class ScrollValuesComputer(context: Context) : DurationValuesComputer(context) {
        override fun computePlayPauseIcon() {
            playPauseIcon = R.drawable.ic_buffering
        }
    }

    private val playValuesComputer: PlayValuesComputer = PlayValuesComputer(context)
    private val scrollValuesComputer: ScrollValuesComputer = ScrollValuesComputer(context)
    private var currentValuesComputer: ValuesComputer = playValuesComputer
        set(value) {
            field.onValuesComputed = {}
            field = value
            field.onValuesComputed = {
                playPauseIcon = getMaybeAnimatedIcon(field.playPauseIcon, playPausePosition)
                        ?: playPauseIcon
                nextIcon = getMaybeAnimatedIcon(field.nextIcon, playPausePosition) ?: nextIcon
                previousIcon = getMaybeAnimatedIcon(field.previousIcon, playPausePosition)
                        ?: previousIcon
                invalidate()
            }
        }

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

    // Variables that can be configured by XML attributes
    private var _currentDuration: Int = 0
    var currentDuration: Int
        set(value) {
            playValuesComputer.duration = value
        }
        get() = currentValuesComputer.duration
    var state = STATE_PAUSE
        set(value) {
            currentValuesComputer.currentState = value
            field = value
        }

    var actionListener: OnActionListener? = null
    var totalDuration: Int = 0
        set(value) {
            field = if (value == 0) Int.MAX_VALUE else value
            scrollValuesComputer.totalDuration = field
            playValuesComputer.totalDuration = field
        }
    var progressAnimDuration: Int = 10000
    var smallestButtonWidth: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICKABLE_SIZE_DP, resources.displayMetrics).toInt()
        set(value) {
            playValuesComputer.smallestButtonWidth = value
            scrollValuesComputer.smallestButtonWidth = value
            field = value
        }

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
    private var previousIcon: AnimatedVectorDrawableCompat
    private var nextIcon: AnimatedVectorDrawableCompat
    private val playPausePosition = Rect()
    private val previousIconPosition: Rect = Rect()
    private val nextIconPosition: Rect = Rect()

    // Calculations
    private var informationBaseline: Int = 0
    private var playButtonMaxWidthOffset: Int
        get() = playValuesComputer.playButtonMaxWidthOffset
        set(value) {
            playValuesComputer.playButtonMaxWidthOffset = value
            scrollValuesComputer.playButtonMaxWidthOffset = value
        }
    private var centerLeftX: Int
        get() = playValuesComputer.centerLeftX
        set(value) {
            playValuesComputer.centerLeftX = value
            scrollValuesComputer.centerLeftX = value
        }
    private var centerRightX: Int
        get() = playValuesComputer.centerRightX
        set(value) {
            playValuesComputer.centerRightX = value
            scrollValuesComputer.centerRightX = value
        }
    private var lastDownEventX = 0f
    private var durationOnScroll = -1
    private var currentScrollOffset = 0

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
        playPauseIcon = getAnimatedIcon(R.drawable.ic_play_to_buffer, playPausePosition)
        previousIcon = getAnimatedIcon(R.drawable.ic_previous_to_start, previousIconPosition)
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
        canvas.drawRect(0f, 0f, currentValuesComputer.playButtonLeftBound.toFloat(), height.toFloat(), previousBackgroundColor)
        canvas.drawRect(currentValuesComputer.playButtonLeftBound.toFloat(), 0f, width - currentValuesComputer.playButtonRightBound.toFloat(), height.toFloat(), backgroundColor)
        canvas.drawRect(width - currentValuesComputer.playButtonRightBound.toFloat(), 0f, width.toFloat(), height.toFloat(), nextBackgroundColor)
        canvas.drawLine(currentValuesComputer.playButtonLeftBound.toFloat(), informationBaseline.toFloat(), width - currentValuesComputer.playButtonRightBound.toFloat(), informationBaseline.toFloat(), timelineOutlineColor)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, textAndOutlineColor)
        playPauseIcon.draw(canvas)
        previousIcon.draw(canvas)
        nextIcon.draw(canvas)
        currentValuesComputer.ticks.filter { it > 0 }.forEach { canvas.drawLine(it, informationBaseline.toFloat(), it, (height / 4 * 3).toFloat(), timelineOutlineColor) }
        canvas.drawText(currentValuesComputer.elapsedDurationText, (smallestButtonWidth / 2).toFloat(), informationBaseline.toFloat(), textAndOutlineColor)
        canvas.drawText(currentValuesComputer.remainingDurationText, (width - (smallestButtonWidth / 2)).toFloat(), informationBaseline.toFloat(), textAndOutlineColor)
        canvas.drawLine(currentValuesComputer.playButtonLeftBound.toFloat(), 0f, currentValuesComputer.playButtonLeftBound.toFloat(), height.toFloat(), textAndOutlineColor)
        canvas.drawLine(width - currentValuesComputer.playButtonRightBound.toFloat(), 0f, width - currentValuesComputer.playButtonRightBound.toFloat(), height.toFloat(), textAndOutlineColor)
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
                } else if (lastDownEventX.toInt() in 0..currentValuesComputer.playButtonLeftBound && event.x.toInt() in 0..currentValuesComputer.playButtonLeftBound) {
                    actionListener?.onPreviousClicked()
                } else if (lastDownEventX.toInt() in currentValuesComputer.playButtonRightBound..width && event.x.toInt() in currentValuesComputer.playButtonRightBound..width) {
                    actionListener?.onNextClicked()
                } else if (lastDownEventX.toInt() in currentValuesComputer.playButtonLeftBound..currentValuesComputer.playButtonRightBound && event.x.toInt() in currentValuesComputer.playButtonLeftBound..currentValuesComputer.playButtonRightBound) {
                    actionListener?.onPlayPauseClicked()
                } else {
                    lastDownEventX = 0f
                    durationOnScroll = -1
                    return super.onTouchEvent(event)
                }
                lastDownEventX = 0f
                durationOnScroll = -1
                currentValuesComputer = playValuesComputer
            }
            MotionEvent.ACTION_MOVE -> {
                currentScrollOffset = (event.x - lastDownEventX).toInt()
                if (currentScrollOffset.absoluteValue > smallestButtonWidth) {
                    currentValuesComputer = scrollValuesComputer
                }
            }
            else -> return super.onTouchEvent(event)
        }
        return true
    }

    /**
     * Private methods
     */

    private fun getMaybeAnimatedIcon(animIconRes: Int?, bounds: Rect): AnimatedVectorDrawableCompat? {
        if (animIconRes == null) {
            return null
        }
        return getAnimatedIcon(animIconRes, bounds)
    }

    private fun getAnimatedIcon(animIconRes: Int, bounds: Rect): AnimatedVectorDrawableCompat {
        val icon = AnimatedVectorDrawableCompat.create(context, animIconRes)
                ?: throw IllegalArgumentException("Icon wasn't found !")
        icon.bounds = bounds
        icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        icon.start()
        return icon
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