package be.florien.anyflow.view.customView

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import be.florien.anyflow.R


internal abstract class DurationPlayerPainter(
        context: Context,
        protected val playPauseIconAnimator: PlayPauseIconAnimator,
        private val previousIconAnimator: PreviousIconAnimator) : PlayerPainter(context) {
    // Sizes
    private var centerLeftX = 0
    private var centerRightX = 0
    private var width = 0
    private var height = 0
    private var informationBaseline: Int = 0

    // Drawables
    private lateinit var nextIcon: AnimatedVectorDrawableCompat
    protected val playPausePosition = Rect()
    private val previousIconPosition: Rect = Rect()
    private val nextIconPosition: Rect = Rect()

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

    // Icon related
    private var iconColor = ContextCompat.getColor(context, R.color.iconInApp)
    private var disabledColor = ContextCompat.getColor(context, R.color.disabled)

    /**
     * Overridden variables
     */
    override var hasPrevious: Boolean = false
        set(value) {
            field = value
            computePreviousIcon()
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

    /**
     * Overridden Methods
     */
    override fun retrieveLayoutProperties(values: TypedArray) {
        smallestButtonWidth = values.getDimensionPixelSize(R.styleable.PlayerControls_smallestButtonWidth, smallestButtonWidth)
        values.getColor(R.styleable.PlayerControls_iconColor, PlayerControls.NO_VALUE).takeIf { it != PlayerControls.NO_VALUE }?.let {
            iconColor = it
        }
        values.getColor(R.styleable.PlayerControls_outLineColor, PlayerControls.NO_VALUE).takeIf { it != PlayerControls.NO_VALUE }?.let {
            textAndOutlineColor.color = it
            timelineOutlineColor.color = it
        }
        values.getColor(R.styleable.PlayerControls_previousBackgroundColor, PlayerControls.NO_VALUE).takeIf { it != PlayerControls.NO_VALUE }?.let {
            previousBackgroundColor.color = it
        }
        values.getColor(R.styleable.PlayerControls_nextBackgroundColor, PlayerControls.NO_VALUE).takeIf { it != PlayerControls.NO_VALUE }?.let {
            nextBackgroundColor.color = it
        }
        values.getColor(R.styleable.PlayerControls_progressBackgroundColor, PlayerControls.NO_VALUE).takeIf { it != PlayerControls.NO_VALUE }?.let {
            backgroundColor.color = it
        }
        values.getColor(R.styleable.PlayerControls_disabledColor, PlayerControls.NO_VALUE).takeIf { it != PlayerControls.NO_VALUE }?.let {
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
        playPauseIconAnimator.icon.draw(canvas)
        previousIconAnimator.icon.draw(canvas)
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
        val state = if (!hasPrevious && duration < progressAnimDuration) PlayerControls.STATE_PREVIOUS_NO_PREVIOUS
        else if (duration > progressAnimDuration) PlayerControls.STATE_PREVIOUS_START
        else PlayerControls.STATE_PREVIOUS_PREVIOUS

        previousIconAnimator.computeIcon(state, previousIconPosition)
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
        val mostRightNextLeft = width - smallestButtonWidth
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

    /**
     * Protected methods
     */

    private fun getAnimatedIcon(animIconRes: Int, bounds: Rect): AnimatedVectorDrawableCompat {
        val icon = AnimatedVectorDrawableCompat.create(context, animIconRes)
                ?: throw IllegalArgumentException("Icon wasn't found !")
        icon.bounds = bounds
        icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        icon.start()
        return icon
    }

    companion object {
        private const val VISIBLE_TEXT_SP = 12f
    }
}