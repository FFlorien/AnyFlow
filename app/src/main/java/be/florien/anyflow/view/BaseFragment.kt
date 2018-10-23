package be.florien.anyflow.view

import android.support.v4.app.Fragment

abstract class BaseFragment: Fragment() {
    abstract fun getTitle(): String
}