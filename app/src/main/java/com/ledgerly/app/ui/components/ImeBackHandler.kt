package com.ledgerly.app.ui.components

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Back pressed with the keyboard up should only hide the keyboard, not close the sheet
 * underneath it. Sheets and dialogs live in their own window, so their own back handling wins
 * over the system's IME dismissal — hence this guard at the top of every sheet with a field.
 */
@Composable
fun ImeBackHandler(onBack: () -> Unit) {
    val view = LocalView.current
    val keyboard = LocalSoftwareKeyboardController.current

    BackHandler(enabled = true) {
        if (isImeVisible(view)) {
            keyboard?.hide()
            view.clearFocus()
        } else {
            onBack()
        }
    }
}

/** True while the soft keyboard occupies part of the window. */
fun isImeVisible(view: View): Boolean {
    val insets = ViewCompat.getRootWindowInsets(view) ?: return false
    return insets.isVisible(WindowInsetsCompat.Type.ime())
}