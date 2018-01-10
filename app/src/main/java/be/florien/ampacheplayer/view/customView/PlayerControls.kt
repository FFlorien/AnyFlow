package be.florien.ampacheplayer.view.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import be.florien.ampacheplayer.R
import android.util.TypedValue
import android.view.MotionEvent
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import java.util.Collections.rotate
import android.R.attr.radius
import android.animation.PropertyValuesHolder
import android.view.animation.LinearInterpolator


private const val CLICKABLE_SIZE_DP = 48f

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
    var currentDuration: Int = 0
        set(value) {
            field = value
            val nextPreviousRight = if (value < transitionDuration) {
                smallestButtonWidthPixel + progressMaxWidthOffset - (((currentDuration.toFloat() / transitionDuration.toFloat()) * progressMaxWidthOffset).toInt())
            } else {
                smallestButtonWidthPixel
            }
            val mostRightNextLeft = width - smallestButtonWidthPixel
            val nextNextLeft = if (value > totalDuration - transitionDuration) {
                mostRightNextLeft - progressMaxWidthOffset + ((((totalDuration - currentDuration).toFloat() / transitionDuration.toFloat()) * progressMaxWidthOffset).toInt())
            } else {
                mostRightNextLeft
            }
            val propertyRadius = PropertyValuesHolder.ofInt("previous", currentPreviousRight, nextPreviousRight)
            val propertyRotate = PropertyValuesHolder.ofInt("next", currentNextLeft, nextNextLeft)

            if (value != 0) {
                currentAnimator = ValueAnimator()
                currentAnimator?.interpolator = LinearInterpolator()
                currentAnimator?.setValues(propertyRadius, propertyRotate)
                currentAnimator?.duration = 1000
                currentAnimator?.addUpdateListener({ animation ->
                    if(currentDuration != 0) {
                        currentPreviousRight = animation.getAnimatedValue("previous") as Int
                        currentNextLeft = animation.getAnimatedValue("next") as Int
                        invalidate()
                    }
                })
                currentAnimator?.start()
            } else {
                currentPreviousRight = nextPreviousRight
                currentNextLeft = nextNextLeft
                invalidate()
            }
        }
    var totalDuration: Int = 0
    var marksOffsets: Int = 0
    var transitionDuration: Int = 10
    var transitionAnimationDuration: Int = 0
    var smallestButtonWidthPixel: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICKABLE_SIZE_DP, resources.displayMetrics).toInt()

    // Variable used for drawing
    private val nextPreviousPaint = Paint().apply {
        setARGB(255, 65, 65, 65)
    }
    private var progressMaxWidthOffset: Int = 0
    private var currentPreviousRight: Int = 0
    private var currentNextLeft: Int = 0
    private var currentAnimator: ValueAnimator? = null

    // Variables used for gestures
    private var lastDownEventX = 0f
    var onPreviousClicked: OnPreviousClickedListener? = null
    var onNextClicked: OnNextClickedListener? = null
    var onPlayPauseClicked: OnPlayPauseClickedListener? = null


    init {
        if (attrs != null) {
            val typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PlayerControls)
            currentDuration = typedArray.getInt(R.styleable.PlayerControls_currentDuration, currentDuration)
            totalDuration = typedArray.getInt(R.styleable.PlayerControls_totalDuration, totalDuration)
            marksOffsets = typedArray.getInt(R.styleable.PlayerControls_marksOffsets, marksOffsets)
            transitionDuration = typedArray.getInt(R.styleable.PlayerControls_transitionDuration, transitionDuration)
            transitionAnimationDuration = typedArray.getInt(R.styleable.PlayerControls_transitionAnimationDuration, transitionAnimationDuration)
            smallestButtonWidthPixel = typedArray.getDimensionPixelSize(R.styleable.PlayerControls_smallestButtonWidth, smallestButtonWidthPixel)
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

        if (desiredWidth < (smallestButtonWidthPixel * 3)) {
            smallestButtonWidthPixel = desiredWidth / 3
        }
        progressMaxWidthOffset = ((desiredWidth - smallestButtonWidthPixel) / 2) - smallestButtonWidthPixel
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0.toFloat(), 0.toFloat(), currentPreviousRight.toFloat(), height.toFloat(), nextPreviousPaint)
        canvas.drawRect(currentNextLeft.toFloat(), 0.toFloat(), width.toFloat(), height.toFloat(), nextPreviousPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> lastDownEventX = event.x
            MotionEvent.ACTION_UP -> {
                if (lastDownEventX in 0..smallestButtonWidthPixel && event.x in 0..smallestButtonWidthPixel) {
                    onPreviousClicked?.onPreviousClicked()
                } else if (lastDownEventX in (width - smallestButtonWidthPixel)..width && event.x in (width - smallestButtonWidthPixel)..width) {
                    onNextClicked?.onNextClicked()
                } else if (lastDownEventX in smallestButtonWidthPixel..(width - smallestButtonWidthPixel) && event.x in smallestButtonWidthPixel..(width - smallestButtonWidthPixel)) {
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