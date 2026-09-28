package com.example.criminalintent

import androidx.appcompat.app.AppCompatActivity

/**
 * Single activity. The FragmentContainerView in activity_main.xml hosts
 * CrimeListFragment first; CrimeDetailFragment replaces it on the back stack.
 * Fragment state (including across rotation) is restored by the FragmentManager.
 */
class MainActivity : AppCompatActivity(R.layout.activity_main)
