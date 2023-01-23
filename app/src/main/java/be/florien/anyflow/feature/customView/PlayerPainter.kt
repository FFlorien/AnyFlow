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
import be.florien.anyflow.player.WaveFormRepository

internal abstract class PlayerPainter(
    val context: Context,
    protected val playPauseIconAnimator: PlayPauseIconAnimator,
    private val previousIconAnimator: PreviousIconAnimator
) {

    // Public values
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
            computeWaveForm()
            computeTicks()
            onValuesComputed()
        }
    var waveForm = DoubleArray(0)
        set(value) {
            field = value
            computeWaveForm()
        }
    var totalDuration = 0
    var currentState = PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE
        set(value) {
            oldState = field
            field = value
            computePlayPauseIcon()
            onValuesComputed()
        }
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
    var onValuesComputed: () -> Unit = {}

    // State
    private var oldState = PlayPauseIconAnimator.STATE_PLAY_PAUSE_PAUSE

    // Buttons
    private var elapsedDurationText = ""
    private var remainingDurationText = ""
    protected var playButtonLeftBound = 0F
    protected var playButtonRightBound = 0F
    var measuredSmallestButtonWidth = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        CLICKABLE_SIZE_DP,
        context.resources.displayMetrics
    )
    protected var measuredPlayButtonOffsetWidth = 0F
    private var measuredSmallPlayButtonLeftX = 0F
    private var measuredSmallPlayButtonRightX = 0F
    private var measuredBigPlayButtonRightX = 0F

    // Timeline
    private val ticks = FloatArray(6)
    private val readWaveFormLeftValues = FloatArray(MAX_DURATION_VISIBLE_IN_SEC + 2) { -1F }
    private val comingWaveFormLeftValues = FloatArray(MAX_DURATION_VISIBLE_IN_SEC + 2) { -1F }
    private var firstWaveFormBarIndex = 0
    private var currentWaveFormBarIndex = 0
    private var lastWaveFormBarIndex = 0
    private var measuredNumberOfWaveFormBarsInHalf = 0
    private var measuredInformationBaseline = 0F
    private var measuredWaveFormBarWidth = 0F
    private var measuredWaveFormBarWidthNoMargin = 0F
    private var measuredCenterX = 0F
    private var measuredTickHeight = 0F
    private var measuredStartEndAnimDuration = 10000

    // Drawables
    private lateinit var nextIcon: AnimatedVectorDrawableCompat
    protected val playPausePosition = Rect()
    private val previousIconPosition = Rect()
    private val nextIconPosition = Rect()
    private var iconColor = ContextCompat.getColor(context, R.color.iconInApp)
    private var disabledColor = ContextCompat.getColor(context, R.color.disabled)

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
    private val readWaveFormBarsColor = Paint().apply {
        val color = ContextCompat.getColor(context, R.color.primaryDark)
        setColor(color)
        strokeWidth = 2f
    }
    private val comingWaveFormBarsColor = Paint().apply {
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

    // Abstract methods

    protected abstract fun computePlayPauseIcon()
    abstract fun getButtonClicked(lastDownEventX: Int, downEventX: Int): Int

    /**
     * Widget Methods
     */
    open fun retrieveLayoutProperties(values: TypedArray) {
        measuredSmallestButtonWidth = values.getDimensionPixelSize(
            R.styleable.PlayerControls_smallestButtonWidth,
            measuredSmallestButtonWidth.toInt()
        ).toFloat()
        values.getColor(R.styleable.PlayerControls_iconColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                iconColor = it
            }
        values.getColor(R.styleable.PlayerControls_outLineColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                textAndOutlineColor.color = it
                timelineOutlineColor.color = it
            }
        values.getColor(R.styleable.PlayerControls_readBarsColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                readWaveFormBarsColor.color = it
            }
        values.getColor(R.styleable.PlayerControls_comingBarsColor, PlayerControls.NO_VALUE)
            .takeIf { it != PlayerControls.NO_VALUE }?.let {
                comingWaveFormBarsColor.color = it
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
            measuredSmallestButtonWidth = width / 3f
        }
        measuredCenterX = width / 2f
        val availableWaveFormWidth = width - (measuredSmallestButtonWidth * 2)
        val numberOfWaveFormBars = MAX_DURATION_VISIBLE_IN_SEC * 2f
        val totalMargin = BAR_MARGIN * 2f
        measuredWaveFormBarWidth = availableWaveFormWidth / numberOfWaveFormBars
        measuredWaveFormBarWidthNoMargin = measuredWaveFormBarWidth - totalMargin
        measuredSmallPlayButtonLeftX = ((width - measuredSmallestButtonWidth) / 2)
        measuredSmallPlayButtonRightX = measuredSmallPlayButtonLeftX + measuredSmallestButtonWidth
        measuredBigPlayButtonRightX = width - measuredSmallestButtonWidth
        measuredPlayButtonOffsetWidth = measuredSmallPlayButtonLeftX - measuredSmallestButtonWidth
        measuredStartEndAnimDuration =
            ((measuredPlayButtonOffsetWidth * BAR_DURATION_MS) / measuredWaveFormBarWidth).toInt()
        measuredNumberOfWaveFormBarsInHalf =
            ((width - (measuredSmallestButtonWidth * 2)) / 2 / measuredWaveFormBarWidth).toInt()

        measuredTickHeight = height / 4f * 3f
        val iconMargin = (measuredSmallestButtonWidth / 4).toInt()
        val iconSize = (measuredSmallestButtonWidth / 2).toInt()
        val playIconStartX = (measuredSmallPlayButtonLeftX + iconMargin).toInt()
        playPausePosition.left = playIconStartX
        playPausePosition.top = iconMargin
        playPausePosition.right = playIconStartX + iconSize
        playPausePosition.bottom = iconMargin + iconSize
        previousIconPosition.left = iconMargin
        previousIconPosition.top = iconMargin
        previousIconPosition.right = iconMargin + iconSize
        previousIconPosition.bottom = iconMargin + iconSize
        val nextIconStartX = (width - measuredSmallestButtonWidth + iconMargin).toInt()
        nextIconPosition.left = nextIconStartX
        nextIconPosition.top = iconMargin
        nextIconPosition.right = nextIconStartX + iconSize
        nextIconPosition.bottom = iconMargin + iconSize
        nextIcon.bounds = nextIconPosition
        measuredInformationBaseline = (height - 10f)
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        val heightFloat = height.toFloat()
        val widthFloat = width.toFloat()
        canvas.drawRect( // Previous background
            /* left =   */ 0f,
            /* top =    */ 0f,
            /* right =  */ playButtonLeftBound,
            /* bottom = */ heightFloat,
            /* paint =  */ previousBackgroundColor
        )
        canvas.drawRect( // Play/pause background
            /* left =   */ playButtonLeftBound,
            /* top =    */ 0f,
            /* right =  */ playButtonRightBound,
            /* bottom = */ heightFloat,
            /* paint =  */ backgroundColor
        )
        canvas.drawRect( // Next background
            /* left =   */ playButtonRightBound,
            /* top =    */ 0f,
            /* right =  */ widthFloat,
            /* bottom = */ heightFloat,
            /* paint =  */ nextBackgroundColor
        )
        canvas.drawLine( // Widget top line (separator)
            /* startX = */ 0f,
            /* startY = */ 0f,
            /* stopX =  */ widthFloat,
            /* stopY =  */ 0f,
            /* paint =  */ textAndOutlineColor
        )
        readWaveFormLeftValues.forEachIndexed { index, value ->
            if (value < 0 || waveForm.isEmpty()) {
                return@forEachIndexed
            }
            val barRatio = waveForm[index + firstWaveFormBarIndex]
            val topBound = (measuredInformationBaseline - (measuredInformationBaseline * barRatio))
            val leftBound = (value + BAR_MARGIN)
            val rightBound = value + measuredWaveFormBarWidth - BAR_MARGIN

            canvas.drawRect( // Already played Amplitude bars
                /* left =   */ leftBound.coerceAtLeast(playButtonLeftBound),
                /* top =    */ topBound.toFloat(),
                /* right =  */ rightBound.coerceAtMost(measuredCenterX),
                /* bottom = */ measuredInformationBaseline,
                /* paint =  */ readWaveFormBarsColor
            )
        }
        comingWaveFormLeftValues.forEachIndexed { index, value ->
            if (value < 0 || waveForm.isEmpty()) {
                return@forEachIndexed
            }
            val barRatio = waveForm[index + currentWaveFormBarIndex]
            val topBound = (measuredInformationBaseline - (measuredInformationBaseline * barRatio))
            val leftBound = (value + BAR_MARGIN)
            val rightBound = value + measuredWaveFormBarWidth - BAR_MARGIN
            canvas.drawRect( // Amplitude bars to read
                /* left =   */ leftBound.coerceAtLeast(measuredCenterX),
                /* top =    */ topBound.toFloat(),
                /* right =  */ rightBound.coerceAtMost(playButtonRightBound),
                /* bottom = */ measuredInformationBaseline,
                /* paint =  */ comingWaveFormBarsColor
            )
        }
        playPauseIconAnimator.icon.draw(canvas)
        previousIconAnimator.icon.draw(canvas)
        nextIcon.draw(canvas)
        ticks.filter { it > 0 }.forEach {
            canvas.drawLine( // Ticks
                /* startX = */ it,
                /* startY = */ measuredInformationBaseline,
                /* stopX =  */ it,
                /* stopY =  */ measuredTickHeight,
                /* paint =  */ timelineOutlineColor
            )
        }
        canvas.drawLine( // Bottom time line
            /* startX = */ playButtonLeftBound,
            /* startY = */ measuredInformationBaseline,
            /* stopX =  */ playButtonRightBound,
            /* stopY =  */ measuredInformationBaseline,
            /* paint =  */ timelineOutlineColor
        )
        canvas.drawText( // Elapsed time text
            /* text =  */ elapsedDurationText,
            /* x =     */ (measuredSmallestButtonWidth / 2F),
            /* y =     */ measuredInformationBaseline,
            /* paint = */ textAndOutlineColor
        )
        canvas.drawText( // Remaining time text
            /* text =  */ remainingDurationText,
            /* x =     */ (widthFloat - (measuredSmallestButtonWidth / 2)),
            /* y =     */ measuredInformationBaseline,
            /* paint = */ textAndOutlineColor
        )
        canvas.drawLine( // Previous/Play button separator
            /* startX = */ playButtonLeftBound,
            /* startY = */ 0f,
            /* stopX =  */ playButtonLeftBound,
            /* stopY =  */ heightFloat,
            /* paint =  */ textAndOutlineColor
        )
        canvas.drawLine( // Play/Next button separator
            /* startX = */ playButtonRightBound,
            /* startY = */ 0f,
            /* stopX =  */ playButtonRightBound,
            /* stopY =  */ heightFloat,
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
            val startProgressPercent = duration.toFloat() / measuredStartEndAnimDuration
            val offsetOfPlayButton = (startProgressPercent * measuredPlayButtonOffsetWidth).toInt()
            val maybeLeftBound = (measuredSmallPlayButtonLeftX - offsetOfPlayButton).coerceAtMost(
                measuredSmallPlayButtonLeftX
            )
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
            val endProgressPercent = remainingTime / measuredStartEndAnimDuration
            val offsetOfPlayButton = (endProgressPercent * measuredPlayButtonOffsetWidth).toInt()
            val maybeRightBound = (measuredSmallPlayButtonRightX + offsetOfPlayButton).coerceAtLeast(measuredSmallPlayButtonRightX)
            if (maybeRightBound < measuredBigPlayButtonRightX) {
                maybeRightBound
            } else {
                measuredBigPlayButtonRightX
            }
        } else {
            measuredBigPlayButtonRightX
        }
    }

    private fun computeWaveForm() {
        if (measuredCenterX == 0F || waveForm.isEmpty()) {
            return
        }

        val barPositionInDuration = duration % BAR_DURATION_MS
        val barProgressPercent = barPositionInDuration.toFloat() / BAR_DURATION_MS
        val barStartOffset = (barProgressPercent * measuredWaveFormBarWidth).toInt()

        currentWaveFormBarIndex =
            (duration / BAR_DURATION_MS).coerceAtLeast(0).coerceAtMost(waveForm.size - 1)
        firstWaveFormBarIndex = (currentWaveFormBarIndex - measuredNumberOfWaveFormBarsInHalf).coerceAtLeast(0)
        lastWaveFormBarIndex =
            (currentWaveFormBarIndex + measuredNumberOfWaveFormBarsInHalf + 1).coerceAtMost(waveForm.size - 1)
        val firstCurrentDelta = currentWaveFormBarIndex - firstWaveFormBarIndex

        waveForm.slice(firstWaveFormBarIndex..currentWaveFormBarIndex).forEachIndexed { index, _ ->
            readWaveFormLeftValues[index] =
                (measuredCenterX - ((firstCurrentDelta - index) * measuredWaveFormBarWidth) - barStartOffset)
        }

        waveForm.slice(currentWaveFormBarIndex until lastWaveFormBarIndex).forEachIndexed { index, _ ->
            comingWaveFormLeftValues[index] =
                (measuredCenterX + (index * measuredWaveFormBarWidth) - barStartOffset)
        }

        val readBarsSize = currentWaveFormBarIndex - firstWaveFormBarIndex + 1
        readWaveFormLeftValues.fill(-1F, readBarsSize)

        val comingBarsSize = (lastWaveFormBarIndex - currentWaveFormBarIndex)
        comingWaveFormLeftValues.fill(-1F, comingBarsSize)
    }

    private fun computeTicks() {
        val tickOffset = measuredWaveFormBarWidth * 10
        val tickPositionInDuration = duration % 5000
        val tickProgressPercent = tickPositionInDuration.toFloat() / 5000
        val tickStartOffset = (tickProgressPercent * tickOffset).toInt()
        val firstTickX = measuredCenterX - tickStartOffset - (tickOffset * 2)
        for (i in 0 until 6) {
            val maybeTickX = firstTickX + (tickOffset * i)
            ticks[i] = when (maybeTickX) {
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
        private const val BAR_MARGIN = 1F
        private const val BAR_DURATION_MS = WaveFormRepository.BAR_DURATION_MS


        const val CLICK_PREVIOUS = 0
        const val CLICK_PLAY_PAUSE = 1
        const val CLICK_NEXT = 2
        const val CLICK_SCROLL = 3
        const val CLICK_UNKNOWN = -1
        private const val CLICKABLE_SIZE_DP = 48f

    }
}