package be.florien.anyflow.feature.playlist.songs

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import be.florien.anyflow.R

fun Context.removeSongsConfirmation(vm: RemoveSongsViewModel) {
    AlertDialog.Builder(this)
        .setTitle(R.string.info_action_remove_from_playlist)
        .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
            vm.removeSongs()
        }
        .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
            dialog.cancel()
        }
        .show()
}

interface RemoveSongsViewModel {
    fun removeSongs()
}