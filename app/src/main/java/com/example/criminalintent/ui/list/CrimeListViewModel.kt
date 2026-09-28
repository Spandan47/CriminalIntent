package com.example.criminalintent.ui.list

import androidx.lifecycle.ViewModel
import com.example.criminalintent.data.Crime
import com.example.criminalintent.data.CrimeRepository
import kotlinx.coroutines.flow.Flow

class CrimeListViewModel : ViewModel() {
    private val repository = CrimeRepository.get()

    /** Emits a fresh list every time the crime table changes. */
    val crimes: Flow<List<Crime>> = repository.getCrimes()

    suspend fun addCrime(crime: Crime) = repository.addCrime(crime)
}
