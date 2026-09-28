package com.example.criminalintent.util

import android.text.format.DateFormat
import java.util.Date

private const val DISPLAY_PATTERN = "EEEE, MMM d, yyyy"

fun Date.toDisplayString(): String = DateFormat.format(DISPLAY_PATTERN, this).toString()
