package be.florien.ampacheplayer.view

import android.support.design.widget.Snackbar
import android.view.View
import javax.inject.Inject

class DisplayHelper(@set:Inject private var rootView: View) {
    fun notifyUserAboutError(error: String) {
        Snackbar.make(rootView, error, Snackbar.LENGTH_SHORT).show()
    }
}
