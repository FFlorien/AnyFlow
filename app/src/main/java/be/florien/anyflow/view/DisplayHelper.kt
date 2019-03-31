package be.florien.anyflow.view

import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import android.view.View
import be.florien.anyflow.di.ActivityScope
import javax.inject.Inject

@ActivityScope
class DisplayHelper
@Inject constructor(private var rootView: View) {

    fun notifyUserAboutError(@StringRes error: Int) {
        Snackbar.make(rootView, error, Snackbar.LENGTH_SHORT).show()
    }
}
