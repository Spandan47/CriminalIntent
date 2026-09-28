package com.example.criminalintent.ui.detail

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

/** Date picker shown in a DialogFragment so it survives rotation. Reports back via the Fragment Result API. */
class DatePickerFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val initialMillis = requireArguments().getLong(ARG_DATE)
        val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val resultDate = GregorianCalendar(year, month, day).time
            setFragmentResult(REQUEST_KEY_DATE, bundleOf(BUNDLE_KEY_DATE to resultDate.time))
        }

        return DatePickerDialog(
            requireContext(),
            dateListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    companion object {
        const val TAG = "DatePickerFragment"
        const val REQUEST_KEY_DATE = "REQUEST_KEY_DATE"
        const val BUNDLE_KEY_DATE = "BUNDLE_KEY_DATE"
        private const val ARG_DATE = "ARG_DATE"

        fun newInstance(date: Date): DatePickerFragment = DatePickerFragment().apply {
            arguments = bundleOf(ARG_DATE to date.time)
        }
    }
}
