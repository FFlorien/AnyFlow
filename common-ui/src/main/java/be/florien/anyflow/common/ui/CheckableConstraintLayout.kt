package be.florien.anyflow.common.ui

import android.R
import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.constraintlayout.widget.ConstraintLayout

class CheckableConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes), Checkable {
    private var isChecked = false
    override fun setChecked(p0: Boolean) {
        isChecked = p0
        refreshDrawableState();
    }

    override fun isChecked() = isChecked

    override fun toggle() {
        setChecked(!isChecked)
    }
    private val checkedStateSet = intArrayOf(
        R.attr.state_checked
    )

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked()) {
            mergeDrawableStates(drawableState, checkedStateSet)
        }
        return drawableState
    }
}