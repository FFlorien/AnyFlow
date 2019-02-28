package be.florien.anyflow.view.customView

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import be.florien.anyflow.R


/**
 * Created by florien on 8/01/18.
 */
class PlayerControls
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    /**
     * Attributes
     */

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
            state = typedArray.getInteger(R.styleable.PlayerControls_state, STATE_PAUSE)
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
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        val idealButtonSize = currentPlayerPainter.smallestButtonWidth

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
                scrollPlayerPainter.durationOnScrollStart = currentDuration
                currentPlayerPainter = scrollPlayerPainter
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
        const val NO_VALUE = -15
    }
}