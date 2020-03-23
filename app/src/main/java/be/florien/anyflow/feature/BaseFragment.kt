package be.florien.anyflow.feature

import androidx.fragment.app.Fragment

abstract class BaseFragment: Fragment() {
    abstract fun getTitle(): String
}