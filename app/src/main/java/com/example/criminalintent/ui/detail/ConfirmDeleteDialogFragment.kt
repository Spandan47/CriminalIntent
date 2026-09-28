package com.example.criminalintent.ui.detail

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.criminalintent.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Asks before deleting so a stray tap can't destroy a record. */
class ConfirmDeleteDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_crime_title)
            .setMessage(R.string.delete_crime_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                setFragmentResult(REQUEST_KEY_CONFIRM_DELETE, bundleOf())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

    companion object {
        const val TAG = "ConfirmDeleteDialogFragment"
        const val REQUEST_KEY_CONFIRM_DELETE = "REQUEST_KEY_CONFIRM_DELETE"
    }
}
