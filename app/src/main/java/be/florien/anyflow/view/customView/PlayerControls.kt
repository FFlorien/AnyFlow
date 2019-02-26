package be.florien.anyflow.view.customView

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
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
    abstract class PlayerPainter(val context: Context) {
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
        var totalDuration: Int = 0
        var currentState: Int = STATE_PAUSE
            set(value) {
                oldState = field
                field = value
                computePlayPauseIcon()
            }
        protected var oldState: Int = STATE_PAUSE

        protected var shouldShowBuffering: Boolean = false
        protected var elapsedDurationText: String = ""
        protected var remainingDurationText: String = ""
        var playButtonMaxWidthOffset = 0 //todo privatize this
        protected val progressAnimDuration: Int = 10000
        var playButtonLeftBound: Int = 0 //todo privatize this
        var playButtonRightBound: Int = 0 //todo privatize this
        protected val ticks: FloatArray = FloatArray(6)
        var onValuesComputed: () -> Unit = {}

        abstract fun retrieveLayoutProperties(values: TypedArray)
        abstract fun measure(width: Int, height: Int)
        abstract fun draw(canvas: Canvas, width: Int, height: Int)
        protected abstract fun computeElapsedDurationText()
        protected abstract fun computeRemainingDurationText()
        protected abstract fun computePlayButtonLeftBound()
        protected abstract fun computePlayButtonRightBound()
        protected abstract fun computeTicks()

        protected abstract fun computePreviousIcon()
        protected abstract fun computePlayPauseIcon()
        abstract var hasPrevious: Boolean
        abstract var hasNext: Boolean
    }

    private abstract class DurationPlayerPainter(context: Context) : PlayerPainter(context) {
        private var smallestButtonWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICKABLE_SIZE_DP, context.resources.displayMetrics).toInt()
        private var centerLeftX = 0
        private var centerRightX = 0
        private var width = 0
        private var height = 0

        // Drawables
        protected lateinit var playPauseIcon: AnimatedVectorDrawableCompat
        protected lateinit var previousIcon: AnimatedVectorDrawableCompat
        protected lateinit var nextIcon: AnimatedVectorDrawableCompat
        protected val playPausePosition = Rect()
        protected val previousIconPosition: Rect = Rect()
        protected val nextIconPosition: Rect = Rect()

        // Paints
        private val textAndOutlineColor = Paint().apply {
            val color = ContextCompat.getColor(context, R.color.iconInApp)
            setColor(color)
            textAlign = Paint.Align.CENTER
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, VISIBLE_TEXT_SP, context.resources.displayMetrics)
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

        private var isPreviousIconPrevious = false
        private var informationBaseline: Int = 0
        override var hasPrevious: Boolean = false
            set(value) {
                field = value
                if (value) {
                    previousIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                } else if (duration < progressAnimDuration) {
                    previousIcon.setColorFilter(disabledColor, PorterDuff.Mode.SRC_IN)
                }
            }
        override var hasNext: Boolean = false
            set(value) {
                field = value
                if (value) {
                    nextIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                } else {
                    nextIcon.setColorFilter(disabledColor, PorterDuff.Mode.SRC_IN)
                }
            }

        override fun retrieveLayoutProperties(values: TypedArray) {
            values.getColor(R.styleable.PlayerControls_iconColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                iconColor = it
            }
            values.getColor(R.styleable.PlayerControls_outLineColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                textAndOutlineColor.color = it
                timelineOutlineColor.color = it
            }
            values.getColor(R.styleable.PlayerControls_previousBackgroundColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                previousBackgroundColor.color = it
            }
            values.getColor(R.styleable.PlayerControls_nextBackgroundColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                nextBackgroundColor.color = it
            }
            values.getColor(R.styleable.PlayerControls_progressBackgroundColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                backgroundColor.color = it
            }
            values.getColor(R.styleable.PlayerControls_disabledColor, NO_VALUE).takeIf { it != NO_VALUE }?.let {
                disabledColor = it
            }
            computePlayPauseIcon()
            computePreviousIcon()
            nextIcon = getAnimatedIcon(R.drawable.ic_next, nextIconPosition)
        }

        override fun measure(width: Int, height: Int) {
            if (width < (smallestButtonWidth * 3)) {
                smallestButtonWidth = width / 3
            }
            centerLeftX = ((width - smallestButtonWidth).toFloat() / 2).toInt()
            centerRightX = centerLeftX + smallestButtonWidth
            playButtonMaxWidthOffset = centerLeftX - smallestButtonWidth
            val margin = smallestButtonWidth / 4
            val iconSize = smallestButtonWidth / 2
            val startX = (((width - smallestButtonWidth) / 2) + margin)
            playPausePosition.left = startX
            playPausePosition.top = margin
            playPausePosition.right = (startX + iconSize)
            playPausePosition.bottom = (margin + iconSize)
            previousIconPosition.left = margin
            previousIconPosition.top = margin
            previousIconPosition.right = (margin + iconSize)
            previousIconPosition.bottom = (margin + iconSize)
            val nextStartX = (width - smallestButtonWidth + margin)
            nextIconPosition.left = nextStartX
            nextIconPosition.top = margin
            nextIconPosition.right = (nextStartX + iconSize)
            nextIconPosition.bottom = margin + iconSize
            playPauseIcon.bounds = playPausePosition
            previousIcon.bounds = previousIconPosition
            nextIcon.bounds = nextIconPosition
            informationBaseline = (height - 10f).toInt()
            this.width = width
            this.height = height
        }

        override fun draw(canvas: Canvas, width: Int, height: Int) {
            canvas.drawRect(0f, 0f, playButtonLeftBound.toFloat(), height.toFloat(), previousBackgroundColor)
            canvas.drawRect(playButtonLeftBound.toFloat(), 0f, playButtonRightBound.toFloat(), height.toFloat(), backgroundColor)
            canvas.drawRect(playButtonRightBound.toFloat(), 0f, width.toFloat(), height.toFloat(), nextBackgroundColor)
            canvas.drawLine(playButtonLeftBound.toFloat(), informationBaseline.toFloat(), playButtonRightBound.toFloat(), informationBaseline.toFloat(), timelineOutlineColor)
            canvas.drawLine(0f, 0f, width.toFloat(), 0f, textAndOutlineColor)
            playPauseIcon.draw(canvas)
            previousIcon.draw(canvas)
            nextIcon.draw(canvas)
            ticks.filter { it > 0 }.forEach { canvas.drawLine(it, informationBaseline.toFloat(), it, (height / 4 * 3).toFloat(), timelineOutlineColor) }
            canvas.drawText(elapsedDurationText, (smallestButtonWidth / 2).toFloat(), informationBaseline.toFloat(), textAndOutlineColor)
            canvas.drawText(remainingDurationText, (width - (smallestButtonWidth / 2)).toFloat(), informationBaseline.toFloat(), textAndOutlineColor)
            canvas.drawLine(playButtonLeftBound.toFloat(), 0f, playButtonLeftBound.toFloat(), height.toFloat(), textAndOutlineColor)
            canvas.drawLine(playButtonRightBound.toFloat(), 0f, playButtonRightBound.toFloat(), height.toFloat(), textAndOutlineColor)

        }

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
            if (duration > progressAnimDuration && isPreviousIconPrevious) {
                isPreviousIconPrevious = false
                previousIcon = getAnimatedIcon(R.drawable.ic_previous_to_start, previousIconPosition)
            } else if (!isPreviousIconPrevious) {
                isPreviousIconPrevious = true
                previousIcon = getAnimatedIcon(R.drawable.ic_start_to_previous, previousIconPosition)
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
            val mostRightNextLeft = width -smallestButtonWidth
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
            val firstTickX = (centerLeftX + (smallestButtonWidth / 2)) + (percentageOfDuration * playButtonMaxWidthOffset) - playButtonMaxWidthOffset - (smallestButtonWidth / 2)

            for (i in 0 until 6) {
                val maybeTickX = firstTickX + (tickOffset * i)
                ticks[i] = when (maybeTickX.toInt()) {
                    in playButtonLeftBound..playButtonRightBound -> maybeTickX
                    else -> -1f
                }
            }
        }

        protected fun getAnimatedIcon(animIconRes: Int, bounds: Rect): AnimatedVectorDrawableCompat {
            val icon = AnimatedVectorDrawableCompat.create(context, animIconRes)
                    ?: throw IllegalArgumentException("Icon wasn't found !")
            icon.bounds = bounds
            icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            icon.start()
            return icon
        }
    }

    private class PlayPlayerPainter(context: Context) : DurationPlayerPainter(context) {

        private var isFirstTime = true
        override fun computePlayPauseIcon() {
            playPauseIcon = if (currentState != oldState) {
                when {
                    oldState == STATE_BUFFER && currentState == STATE_PLAY -> {
                        //isPlayPauseAnimationInfinite = false
                        getAnimatedIcon(R.drawable.ic_buffer_to_pause, playPausePosition)
                    }
                    oldState == STATE_BUFFER && currentState == STATE_PAUSE -> {
                        //isPlayPauseAnimationInfinite = false
                        getAnimatedIcon(R.drawable.ic_buffer_to_play, playPausePosition)
                    }
                    oldState == STATE_PLAY && currentState == STATE_BUFFER -> {
                        //isPlayPauseAnimationInfinite = true
                        getAnimatedIcon(R.drawable.ic_pause_to_buffer, playPausePosition)
                    }
                    oldState == STATE_PLAY && currentState == STATE_PAUSE -> {
                        //isPlayPauseAnimationInfinite = false
                        getAnimatedIcon(R.drawable.ic_pause_to_play, playPausePosition)
                    }
                    oldState == STATE_PAUSE && currentState == STATE_BUFFER -> {
                        //isPlayPauseAnimationInfinite = true
                        getAnimatedIcon(R.drawable.ic_play_to_buffer, playPausePosition)
                    }
                    oldState == STATE_PAUSE && currentState == STATE_PLAY -> {
                        //isPlayPauseAnimationInfinite = false
                        getAnimatedIcon(R.drawable.ic_play_to_pause, playPausePosition)
                    }
                    else -> {
                        //isPlayPauseAnimationInfinite = true
                        getAnimatedIcon(R.drawable.ic_buffering, playPausePosition)
                    }
                }
            } else {
                if (isFirstTime) {
                    isFirstTime = false
                    getAnimatedIcon(R.drawable.ic_buffer_to_play, playPausePosition)
                } else {
                    playPauseIcon
                }
            }
        }
    }

    private class ScrollPlayerPainter(context: Context) : DurationPlayerPainter(context) {
        override fun computePlayPauseIcon() {
            playPauseIcon = getAnimatedIcon(R.drawable.ic_buffering, playPausePosition)
        }
    }

    private val playPlayerPainter: PlayPlayerPainter = PlayPlayerPainter(context)
    private val scrollPlayerPainter: ScrollPlayerPainter = ScrollPlayerPainter(context)
    private var currentPlayerPainter: PlayerPainter = playPlayerPainter
        set(value) {
            field.onValuesComputed = {}
            value.onValuesComputed = {
                invalidate()
            }
            field = value
        }

    private var smallestButtonWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICKABLE_SIZE_DP, context.resources.displayMetrics).toInt()


    // Variable changing due to usage
    var shouldShowBuffering: Boolean = false
    var hasPrevious: Boolean
        get() = currentPlayerPainter.hasPrevious
        set(value) {
            playPlayerPainter.hasPrevious = value
            scrollPlayerPainter.hasPrevious = value
        }
    var hasNext: Boolean
        get() = currentPlayerPainter.hasNext
        set(value) {
            playPlayerPainter.hasNext = value
            scrollPlayerPainter.hasNext = value
        }

    // Variables that can be configured by XML attributes
    var currentDuration: Int
        set(value) {
            playPlayerPainter.duration = value
        }
        get() = playPlayerPainter.duration
    var state = STATE_PAUSE
        set(value) {
            currentPlayerPainter.currentState = value
            field = value
        }

    var actionListener: OnActionListener? = null
    var totalDuration: Int = 0
        set(value) {
            field = if (value == 0) Int.MAX_VALUE else value
            scrollPlayerPainter.totalDuration = field
            playPlayerPainter.totalDuration = field
        }
    var progressAnimDuration: Int = 10000

    // Calculations
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
            state = typedArray.getInteger(R.styleable.PlayerControls_state, STATE_PAUSE)
            playPlayerPainter.retrieveLayoutProperties(typedArray)
            scrollPlayerPainter.retrieveLayoutProperties(typedArray)
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
        playPlayerPainter.measure(desiredWidth, desiredHeight)
        scrollPlayerPainter.measure(desiredWidth, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        currentPlayerPainter.draw(canvas, width, height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastDownEventX = event.x
                durationOnScroll = currentDuration
            }
            MotionEvent.ACTION_UP -> {
                if (currentScrollOffset.absoluteValue > smallestButtonWidth.absoluteValue) {
                    actionListener?.onCurrentDurationChanged((scrollPlayerPainter.duration).toLong())
                    currentScrollOffset = 0
                } else if (lastDownEventX.toInt() in 0..currentPlayerPainter.playButtonLeftBound && event.x.toInt() in 0..currentPlayerPainter.playButtonLeftBound) {
                    actionListener?.onPreviousClicked()
                } else if (lastDownEventX.toInt() in currentPlayerPainter.playButtonRightBound..width && event.x.toInt() in currentPlayerPainter.playButtonRightBound..width) {
                    actionListener?.onNextClicked()
                } else if (lastDownEventX.toInt() in currentPlayerPainter.playButtonLeftBound..currentPlayerPainter.playButtonRightBound && event.x.toInt() in currentPlayerPainter.playButtonLeftBound..currentPlayerPainter.playButtonRightBound) {
                    actionListener?.onPlayPauseClicked()
                } else {
                    lastDownEventX = 0f
                    durationOnScroll = -1
                    return super.onTouchEvent(event)
                }
                lastDownEventX = 0f
                durationOnScroll = -1
                currentPlayerPainter = playPlayerPainter
            }
            MotionEvent.ACTION_MOVE -> {
                currentScrollOffset = (event.x - lastDownEventX).toInt()
                if (currentScrollOffset.absoluteValue > smallestButtonWidth) {
                    currentPlayerPainter = scrollPlayerPainter
                }
                val durationOffset = (currentScrollOffset.toFloat() / (currentPlayerPainter.playButtonMaxWidthOffset.toFloat() / 2)) * 5000
                scrollPlayerPainter.duration = durationOnScroll - durationOffset.toInt()
            }
            else -> return super.onTouchEvent(event)
        }
        return true
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