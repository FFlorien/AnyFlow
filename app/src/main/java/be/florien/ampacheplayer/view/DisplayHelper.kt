package be.florien.ampacheplayer.view

import android.support.design.widget.Snackbar
import android.view.View
import be.florien.ampacheplayer.di.ActivityScope
import javax.inject.Inject

@ActivityScope
class DisplayHelper
@Inject constructor(private var rootView: View) {
    fun notifyUserAboutError(error: String) {
        Snackbar.make(rootView, error, Snackbar.LENGTH_SHORT).show()
    }
}
