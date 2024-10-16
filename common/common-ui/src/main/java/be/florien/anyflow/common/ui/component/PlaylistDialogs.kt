package be.florien.anyflow.common.ui.component

import android.content.Context
import android.content.DialogInterface
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import be.florien.anyflow.common.resources.R

fun Context.newPlaylist(vm: NewPlaylistViewModel) {
    val editText = EditText(this)
    editText.inputType =
        EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
    AlertDialog.Builder(this)
        .setView(editText)
        .setTitle(R.string.info_action_new_playlist)
        .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
            vm.createPlaylist(editText.text.toString())
        }
        .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
            dialog.cancel()
        }
        .show()
}

fun Context.deletePlaylistConfirmation(vm: DeletePlaylistViewModel) {
    AlertDialog.Builder(this)
        .setTitle(R.string.info_action_delete_playlist)
        .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
            vm.deletePlaylist()
        }
        .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
            dialog.cancel()
        }
        .show()
}

interface NewPlaylistViewModel {
    fun createPlaylist(name: String)
}

interface DeletePlaylistViewModel {
    fun deletePlaylist()
}