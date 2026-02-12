package com.example.taskmanagerapp.ui.common

import android.app.AlertDialog
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.example.taskmanagerapp.R

/**
 * Shared confirmation dialog builder to avoid duplicated dialog logic across fragments.
 */
fun Fragment.showConfirmationDialog(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    @StringRes positiveRes: Int = R.string.yes,
    @StringRes negativeRes: Int = R.string.no,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog.Builder(requireContext())
        .setTitle(titleRes)
        .setMessage(messageRes)
        .setPositiveButton(positiveRes) { _, _ -> onConfirm() }
        .setNegativeButton(negativeRes) { _, _ -> onCancel?.invoke() }
        .setOnDismissListener { onDismiss?.invoke() }
        .show()
}

