package be.florien.anyflow.feature.customView

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import be.florien.anyflow.R
import be.florien.anyflow.player.DownSampleRepository

internal abstract class DurationPlayerPainter(
    val context: Context,
    protected val playPauseIconAnimator: PlayPauseIconAnimator,
    private val previousIconAnimator: PreviousIconAnimator
) {

    //todo reordonate the properties
    var duration = 0
        protected set(value) {
            val oldValue = field
            field = value
            if (measuredStartEndAnimDuration in oldValue..value || measuredStartEndAnimDuration in value..oldValue || value < oldValue) {
                computePreviousIcon()
            }
            computeElapsedDurationText()
            computeRemainingDurationText()

            computePlayButtonLeftBound()
            computePlayButtonRightBound()
            computeBars()
            computeTicks()
            onValuesComputed()
        }
    var downSamples: IntArray = IntArray(0)
        set(value) {
            field = value
            computeBars()
        }
    var totalDuration: Int = 0
    var currentState: Int = PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
        set(value) {
            oldState = field
            field = value
            computePlayPauseIcon()
            onValuesComputed()
        }
    var measuredSmallestButtonWidth = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        CLICKABLE_SIZE_DP,
        context.resources.displayMetrics
    ).toInt()
    var onValuesComputed: () -> Unit = {}

    private var oldState: Int = PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE

    private var elapsedDurationText: String = ""
    private var remainingDurationText: String = ""
    protected var measuredPlayButtonOffsetWidth = 0
    private var measuredNumberOfBarsInHalf = 0
    private var measuredStartEndAnimDuration: Int = 10000
    protected var playButtonLeftBound: Int = 0
    protected var playButtonRightBound: Int = 0
    protected val ticks: FloatArray = FloatArray(6)
    private val readBarsLeftValues: FloatArray =
        FloatArray(MAX_DURATION_VISIBLE_IN_SEC + 2) { -1F }
    private val comingBarsLeftValues: FloatArray =
        FloatArray(MAX_DURATION_VISIBLE_IN_SEC + 2) { -1F }
    private var firstBarIndex: Int = 0
    private var currentBarIndex: Int = 0
    private var lastBarIndex: Int = 0


    // Sizes
    private var measuredSmallPlayButtonLeftX = 0
    private var measuredSmallPlayButtonRightX = 0
    private var measuredBigPlayButtonRightX = 0
    private var measuredInformationBaseline: Int = 0
    private var measuredBarWidth = 0
    private var measuredBarWidthNoMargin = 0
    private var measuredCenterX = 0
    private var measuredTickHeight = 0

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
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            VISIBLE_TEXT_SP,
            context.resources.displayMetrics
        )
        strokeWidth = 2f
    }
    private val timelineOutlineColor = Paint().apply {
        val color = ContextCompat.getColor(context, R.color.iconInApp)
        setColor(color)
        strokeWidth = 2f
    }
    private val readBarsColor = Paint().apply {//todo attr
        val color = ContextCompat.getColor(context, R.color.primaryDark)
        setColor(color)
        strokeWidth = 2f
    }
    private val comingBarsColor = Paint().apply {//todo attr
        val color = ContextCompat.getColor(context, R.color.primaryLight)
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

    protected abstract fun computePlayPauseIcon()
    abstract fun getButtonClicked(lastDownEventX: Int, downEventX: Int): Int

    /**
     * Overridden variables
     */
    var hasPrevious: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                computePreviousIcon()
            }
        }
    var hasNext: Boolean = false
        set(value) {
            field = value
            if (value) {
                nextIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    iconColor,
                    BlendModeCompat.SRC_IN
                )
            } else {
                nextIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    disabledColor,
                    BlendModeCompat.SRC_IN
                )
            }
        }

    /**
     * Widget Methods
     */
    open fun retrieveLayoutProperties(values: TypedArray) {
        measuredSmallestButtonWidth = values.getDimensionPixelSize(
            R.styleable.PlayerControls_smallestButtonWidth,
            measuredSmallestButtonWidth
        )
        values.getColor(R.styleable.PlayerControls_iconColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                iconColor = it
            }
        values.getColor(R.styleable.PlayerControls_outLineColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                textAndOutlineColor.color = it
                timelineOutlineColor.color = it
            }
        values.getColor(R.styleable.PlayerControls_previousBackgroundColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                previousBackgroundColor.color = it
            }
        values.getColor(R.styleable.PlayerControls_nextBackgroundColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                nextBackgroundColor.color = it
            }
        values.getColor(R.styleable.PlayerControls_progressBackgroundColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                backgroundColor.color = it
            }
        values.getColor(R.styleable.PlayerControls_disabledColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                disabledColor = it
            }
        computePlayPauseIcon()
        computePreviousIcon()
        nextIcon = getNextIcon(nextIconPosition)
    }

    fun measure(width: Int, height: Int) {
        if (width < (measuredSmallestButtonWidth * 3)) {
            measuredSmallestButtonWidth = width / 3
        }
        measuredCenterX = width / 2
        val availableBarsWidth = width - (measuredSmallestButtonWidth * 2)
        val numberOfBars = MAX_DURATION_VISIBLE_IN_SEC * 2
        val totalMargin = BAR_MARGIN * 2
        measuredBarWidth = availableBarsWidth / numberOfBars
        measuredBarWidthNoMargin = measuredBarWidth - totalMargin
        measuredSmallPlayButtonLeftX = ((width - measuredSmallestButtonWidth).toFloat() / 2).toInt()
        measuredSmallPlayButtonRightX = measuredSmallPlayButtonLeftX + measuredSmallestButtonWidth
        measuredBigPlayButtonRightX = width - measuredSmallestButtonWidth
        measuredPlayButtonOffsetWidth = measuredSmallPlayButtonLeftX - measuredSmallestButtonWidth
        measuredStartEndAnimDuration =
            (measuredPlayButtonOffsetWidth * BAR_DURATION_MS) / measuredBarWidth
        measuredNumberOfBarsInHalf = (width - (measuredSmallestButtonWidth * 2)) / 2 / measuredBarWidth

        measuredTickHeight = height / 4 * 3
        val iconMargin = measuredSmallestButtonWidth / 4
        val iconSize = measuredSmallestButtonWidth / 2
        val playIconStartX = measuredSmallPlayButtonLeftX + iconMargin
        playPausePosition.left = playIconStartX
        playPausePosition.top = iconMargin
        playPausePosition.right = playIconStartX + iconSize
        playPausePosition.bottom = iconMargin + iconSize
        previousIconPosition.left = iconMargin
        previousIconPosition.top = iconMargin
        previousIconPosition.right = iconMargin + iconSize
        previousIconPosition.bottom = iconMargin + iconSize
        val nextIconStartX = width - measuredSmallestButtonWidth + iconMargin
        nextIconPosition.left = nextIconStartX
        nextIconPosition.top = iconMargin
        nextIconPosition.right = nextIconStartX + iconSize
        nextIconPosition.bottom = iconMargin + iconSize
        nextIcon.bounds = nextIconPosition
        measuredInformationBaseline = (height - 10f).toInt()
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        canvas.drawRect( // Previous background
            /* left =   */ 0f,
            /* top =    */ 0f,
            /* right =  */ playButtonLeftBound.toFloat(),
            /* bottom = */ height.toFloat(),
            /* paint =  */ previousBackgroundColor
        )
        canvas.drawRect( // Play/pause background
            /* left =   */ playButtonLeftBound.toFloat(),
            /* top =    */ 0f,
            /* right =  */ playButtonRightBound.toFloat(),
            /* bottom = */ height.toFloat(),
            /* paint =  */ backgroundColor
        )
        canvas.drawRect( // Next background
            /* left =   */ playButtonRightBound.toFloat(),
            /* top =    */ 0f,
            /* right =  */ width.toFloat(),
            /* bottom = */ height.toFloat(),
            /* paint =  */ nextBackgroundColor
        )
        canvas.drawLine( // Bottom time line
            /* startX = */ playButtonLeftBound.toFloat(),
            /* startY = */ measuredInformationBaseline.toFloat(),
            /* stopX =  */ playButtonRightBound.toFloat(),
            /* stopY =  */ measuredInformationBaseline.toFloat(),
            /* paint =  */ timelineOutlineColor
        )
        canvas.drawLine( // Widget top line (separator)
            /* startX = */ 0f,
            /* startY = */ 0f,
            /* stopX =  */ width.toFloat(),
            /* stopY =  */ 0f,
            /* paint =  */ textAndOutlineColor
        )
        readBarsLeftValues.forEachIndexed { index, value ->
            if (value < 0) {
                return@forEachIndexed
            }
            val barHeight = downSamples[index + firstBarIndex]
            val topBound = (measuredInformationBaseline - barHeight).toFloat()
            val leftBound = (value + BAR_MARGIN)
            val rightBound = value + measuredBarWidth - BAR_MARGIN

            canvas.drawRect( // Already played Amplitude bars
                /* left =   */ leftBound.coerceAtLeast(playButtonLeftBound.toFloat()),
                /* top =    */ topBound,
                /* right =  */ rightBound.coerceAtMost((width / 2).toFloat()),
                /* bottom = */ measuredInformationBaseline.toFloat(),
                /* paint =  */ readBarsColor
            )
        }
        comingBarsLeftValues.forEachIndexed { index, value ->
            if (value < 0) {
                return@forEachIndexed
            }
            val barHeight = downSamples[index + currentBarIndex]
            val topBound = (measuredInformationBaseline - barHeight).toFloat()
            val leftBound = (value + BAR_MARGIN)
            val rightBound = value + measuredBarWidth - BAR_MARGIN
            canvas.drawRect( // Amplitude bars to read
                /* left =   */ leftBound.coerceAtLeast((width / 2).toFloat()),
                /* top =    */ topBound,
                /* right =  */ rightBound.coerceAtMost(playButtonRightBound.toFloat()),
                /* bottom = */ measuredInformationBaseline.toFloat(),
                /* paint =  */ comingBarsColor
            )
        }
        playPauseIconAnimator.icon.draw(canvas)
        previousIconAnimator.icon.draw(canvas)
        nextIcon.draw(canvas)
        ticks.filter { it > 0 }.forEach {
            canvas.drawLine( // Ticks
                /* startX = */ it,
                // todo save all data in float to avoid conversion
                /* startY = */ measuredInformationBaseline.toFloat(),
                /* stopX =  */ it,
                /* stopY =  */ measuredTickHeight.toFloat(),
                /* paint =  */ timelineOutlineColor
            )
        }
        canvas.drawText( // Elapsed time text
            /* text =  */ elapsedDurationText,
            /* x =     */ (measuredSmallestButtonWidth / 2).toFloat(),
            /* y =     */ measuredInformationBaseline.toFloat(),
            /* paint = */ textAndOutlineColor
        )
        canvas.drawText( // Remaining time text
            /* text =  */ remainingDurationText,
            /* x =     */ (width - (measuredSmallestButtonWidth / 2)).toFloat(),
            /* y =     */ measuredInformationBaseline.toFloat(),
            /* paint = */ textAndOutlineColor
        )
        canvas.drawLine( // Previous/Play button separator
            /* startX = */ playButtonLeftBound.toFloat(),
            /* startY = */ 0f,
            /* stopX =  */ playButtonLeftBound.toFloat(),
            /* stopY =  */ height.toFloat(),
            /* paint =  */ textAndOutlineColor
        )
        canvas.drawLine( // Play/Next button separator
            /* startX = */ playButtonRightBound.toFloat(),
            /* startY = */ 0f,
            /* stopX =  */ playButtonRightBound.toFloat(),
            /* stopY =  */ height.toFloat(),
            /* paint =  */ textAndOutlineColor
        )

    }

    /**
     * Computation pre-drawing
     */

    private fun computeElapsedDurationText() {
        val playBackTimeInSeconds = duration / 1000
        val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
        val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
        elapsedDurationText = "$minutesDisplay:$secondsDisplay"
    }

    private fun computeRemainingDurationText() {
        remainingDurationText = if (totalDuration == Int.MAX_VALUE) {
            context.getString(R.string.player_controls_unknown)
        } else {
            val playBackTimeInSeconds = (totalDuration - duration) / 1000
            val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
            val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
            "-$minutesDisplay:$secondsDisplay"
        }
    }

    fun computePreviousIcon() {
        val state = when {
            !hasPrevious && duration < measuredStartEndAnimDuration -> PreviousIconAnimator.STATE_PREVIOUS_NO_PREVIOUS
            duration > measuredStartEndAnimDuration -> PreviousIconAnimator.STATE_PREVIOUS_START
            else -> PreviousIconAnimator.STATE_PREVIOUS_PREVIOUS
        }

        previousIconAnimator.computeIcon(state, previousIconPosition)
    }

    private fun computePlayButtonLeftBound() {
        playButtonLeftBound = if (duration < measuredStartEndAnimDuration) {
            val startProgressPercent = duration.toFloat() / measuredStartEndAnimDuration.toFloat()
            val offsetOfPlayButton = (startProgressPercent * measuredPlayButtonOffsetWidth).toInt()
            val maybeLeftBound = measuredSmallPlayButtonLeftX - offsetOfPlayButton
            if (maybeLeftBound > measuredSmallestButtonWidth) {
                maybeLeftBound
            } else {
                measuredSmallestButtonWidth
            }
        } else {
            measuredSmallestButtonWidth
        }
    }

    private fun computePlayButtonRightBound() {
        playButtonRightBound = if (duration > totalDuration - measuredStartEndAnimDuration) {
            val remainingTime = (totalDuration - duration).toFloat()
            val endProgressPercent = remainingTime / measuredStartEndAnimDuration.toFloat()
            val offsetOfPlayButton = (endProgressPercent * measuredPlayButtonOffsetWidth).toInt()
            val maybeRightBound = measuredSmallPlayButtonRightX + offsetOfPlayButton
            if (maybeRightBound < measuredBigPlayButtonRightX) {
                maybeRightBound
            } else {
                measuredBigPlayButtonRightX
            }
        } else {
            measuredBigPlayButtonRightX
        }
    }

    private fun computeBars() {
        if (measuredCenterX == 0 || downSamples.isEmpty()) {
            return
        }

        val barPositionInDuration = duration % BAR_DURATION_MS
        val barProgressPercent = barPositionInDuration.toFloat() / BAR_DURATION_MS
        val barStartOffset = (barProgressPercent * (measuredBarWidthNoMargin + 2)).toInt()

        currentBarIndex = duration / BAR_DURATION_MS
        firstBarIndex = (currentBarIndex - measuredNumberOfBarsInHalf).coerceAtLeast(0)
        lastBarIndex = (currentBarIndex + measuredNumberOfBarsInHalf + 1).coerceAtMost(downSamples.size - 1)
        val firstCurrentDelta = currentBarIndex - firstBarIndex

        downSamples.slice(firstBarIndex..currentBarIndex).forEachIndexed { index, _ ->
            readBarsLeftValues[index] =
                (measuredCenterX - ((firstCurrentDelta - index) * measuredBarWidth) - barStartOffset).toFloat()
        }

        downSamples.slice(currentBarIndex until lastBarIndex).forEachIndexed { index, _ ->
            comingBarsLeftValues[index] =
                (measuredCenterX + (index * measuredBarWidth) - barStartOffset).toFloat()
        }

        val readBarsSize = (currentBarIndex - firstBarIndex)
        readBarsLeftValues.fill(-1F, readBarsSize)

        val comingBarsSize = (lastBarIndex - currentBarIndex)
        comingBarsLeftValues.fill(-1F, comingBarsSize)
    }

    private fun computeTicks() {
        val tickOffset = measuredPlayButtonOffsetWidth.toFloat() / 2
        val nextTickInDuration = 5000 - (duration % 5000)
        val percentageOfDuration = nextTickInDuration.toFloat() / 10000f
        val firstTickX =
            (measuredSmallPlayButtonLeftX + (measuredSmallestButtonWidth / 2)) + (percentageOfDuration * measuredPlayButtonOffsetWidth) - measuredPlayButtonOffsetWidth - (measuredSmallestButtonWidth / 2)

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

    private fun getNextIcon(bounds: Rect): AnimatedVectorDrawableCompat {
        val icon = AnimatedVectorDrawableCompat.create(context, R.drawable.ic_next)
            ?: throw IllegalArgumentException("Icon wasn't found !")
        icon.bounds = bounds
        icon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            iconColor,
            BlendModeCompat.SRC_IN
        )
        icon.start()
        return icon
    }

    companion object {
        private const val VISIBLE_TEXT_SP = 12f
        private const val MAX_DURATION_VISIBLE_IN_SEC = 25
        private const val BAR_MARGIN = 1
        private const val BAR_DURATION_MS = DownSampleRepository.DOWN_SAMPLE_DURATION_MS


        const val CLICK_PREVIOUS = 0
        const val CLICK_PLAY_PAUSE = 1
        const val CLICK_NEXT = 2
        const val CLICK_SCROLL = 3
        const val CLICK_UNKNOWN = -1
        private const val CLICKABLE_SIZE_DP = 48f

    }
}