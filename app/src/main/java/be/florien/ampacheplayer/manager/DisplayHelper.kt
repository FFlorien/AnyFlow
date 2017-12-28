package be.florien.ampacheplayer.manager

import android.support.design.widget.Snackbar
import android.view.View
import javax.inject.Inject

class DisplayHelper(@Inject private val rootView: View) {
    fun notifyUserAboutError(error: String) {
        Snackbar.make(rootView, error, Snackbar.LENGTH_SHORT).show()
    }
}
