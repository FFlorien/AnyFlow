package be.florien.anyflow.common.ui

import androidx.fragment.app.Fragment

abstract class BaseFragment: Fragment() {
    abstract fun getTitle(): String
    open fun getSubtitle(): String? = null
}