package be.florien.anyflow.view

import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.view.View
import be.florien.anyflow.di.ActivityScope
import javax.inject.Inject

@ActivityScope
class DisplayHelper
@Inject constructor(private var rootView: View) {
    fun notifyUserAboutError(error: String) {
        Snackbar.make(rootView, error, Snackbar.LENGTH_SHORT).show()
    }

    fun notifyUserAboutError(@StringRes error: Int) {
        Snackbar.make(rootView, error, Snackbar.LENGTH_SHORT).show()
    }
}
