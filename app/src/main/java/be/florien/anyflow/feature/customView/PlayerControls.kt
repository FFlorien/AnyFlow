package be.florien.anyflow.feature.customView

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import be.florien.anyflow.R
import kotlin.math.absoluteValue
import kotlin.math.min


/**
 * Created by florien on 8/01/18.
 */
class PlayerControls
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    /**
     * Attributes
     */

    private val playPauseIconAnimator: PlayPauseIconAnimator = PlayPauseIconAnimator(context)
    private val previousIconAnimator: PreviousIconAnimator = PreviousIconAnimator(context)
    private val playPlayerPainter: PlayPlayerPainter = PlayPlayerPainter(context, playPauseIconAnimator, previousIconAnimator)
    private val scrollPlayerPainter: ScrollPlayerPainter = ScrollPlayerPainter(context, playPauseIconAnimator, previousIconAnimator)
    private var currentPlayerPainter: PlayerPainter = playPlayerPainter
        set(value) {
            field.onValuesComputed = {}
            value.onValuesComputed = {
                invalidate()
            }
            field = value
            field.currentState = field.currentState
        }

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
            playPlayerPainter.playingDuration = value
        }
        get() = playPlayerPainter.playingDuration
    var state = PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
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
    private var progressAnimDuration: Int = 10000

    // Calculations
    private var lastDownEventX = 0f

    /**
     * Constructor
     */
    init {
        if (attrs != null) {
            val typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PlayerControls)
            currentDuration = typedArray.getInt(R.styleable.PlayerControls_currentDuration, currentDuration)
            totalDuration = typedArray.getInt(R.styleable.PlayerControls_totalDuration, totalDuration)
            progressAnimDuration = typedArray.getInt(R.styleable.PlayerControls_progressAnimDuration, progressAnimDuration)
            state = typedArray.getInteger(R.styleable.PlayerControls_state, PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE)
            playPlayerPainter.retrieveLayoutProperties(typedArray)
            scrollPlayerPainter.retrieveLayoutProperties(typedArray)
            typedArray.recycle()
        }
        currentPlayerPainter = playPlayerPainter
    }


    /**
     * Overridden methods
     */

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val idealButtonSize = currentPlayerPainter.smallestButtonWidth

        val desiredWidth = when (widthMode) {
            MeasureSpec.AT_MOST -> widthSize
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.UNSPECIFIED -> idealButtonSize
            else -> idealButtonSize
        }

        val desiredHeight = when (heightMode) {
            MeasureSpec.AT_MOST -> min(heightSize, idealButtonSize)
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.UNSPECIFIED -> idealButtonSize
            else -> idealButtonSize
        }

        setMeasuredDimension(desiredWidth, desiredHeight)
        playPlayerPainter.measure(desiredWidth, desiredHeight)
        scrollPlayerPainter.measure(desiredWidth, desiredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        currentPlayerPainter.computePreviousIcon()
    }

    override fun onDraw(canvas: Canvas) {
        currentPlayerPainter.draw(canvas, width, height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastDownEventX = event.x
                scrollPlayerPainter.durationOnScrollStart = currentDuration
            }
            MotionEvent.ACTION_UP -> {
                if (scrollPlayerPainter.getButtonClicked(lastDownEventX.toInt(), event.x.toInt()) == PlayerPainter.CLICK_SCROLL) {
                    actionListener?.onCurrentDurationChanged(scrollPlayerPainter.duration.toLong())
                } else {
                    when (playPlayerPainter.getButtonClicked(lastDownEventX.toInt(), event.x.toInt())) {
                        PlayerPainter.CLICK_PREVIOUS -> actionListener?.onPreviousClicked()
                        PlayerPainter.CLICK_PLAY_PAUSE -> actionListener?.onPlayPauseClicked()
                        PlayerPainter.CLICK_NEXT -> actionListener?.onNextClicked()
                        else -> {
                            lastDownEventX = 0f
                            return super.onTouchEvent(event)
                        }
                    }
                }
                lastDownEventX = 0f
                currentPlayerPainter = playPlayerPainter
            }
            MotionEvent.ACTION_MOVE -> {
                scrollPlayerPainter.scrollOffset = (event.x - lastDownEventX)
                if (scrollPlayerPainter.scrollOffset.absoluteValue > playPlayerPainter.smallestButtonWidth && currentPlayerPainter != scrollPlayerPainter) {
                    currentPlayerPainter = scrollPlayerPainter
                }
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
        const val NO_VALUE = -15
    }
}