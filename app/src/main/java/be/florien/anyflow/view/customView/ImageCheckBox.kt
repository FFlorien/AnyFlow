package be.florien.anyflow.view.customView

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.widget.Checkable
import android.widget.ImageView


class ImageCheckBox : ImageView, Checkable {

    init {
        super.setOnClickListener { toggle() }
    }

    private var mChecked = false

    var onCheckedChangeListener: OnCheckedChangeListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked)
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        return drawableState
    }

    override fun setChecked(checked: Boolean) {
        if (mChecked != checked) {
            mChecked = checked
            onCheckedChangeListener?.onCheckedChanged(this, mChecked)
            refreshDrawableState()
        }
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun toggle() {
        isChecked = !mChecked
    }

    override fun setOnClickListener(listener: OnClickListener) {
        val onClickListener = OnClickListener { v ->
            toggle()
            listener.onClick(v)
        }
        super.setOnClickListener(onClickListener)
    }

    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changed.
     */
    interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param buttonView The compound button view whose state has changed.
         * @param isChecked  The new checked state of buttonView.
         */
        fun onCheckedChanged(buttonView: ImageCheckBox, isChecked: Boolean)
    }

    companion object {

        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }
}