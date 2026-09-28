package com.example.criminalintent.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.criminalintent.data.Crime
import com.example.criminalintent.data.CrimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Holds the crime being edited. Because the ViewModel survives rotation, edits
 * live here and are written to the database when the screen stops.
 */
class CrimeDetailViewModel(crimeId: UUID) : ViewModel() {
    private val repository = CrimeRepository.get()

    private val _crime = MutableStateFlow<Crime?>(null)
    val crime: StateFlow<Crime?> = _crime.asStateFlow()

    init {
        viewModelScope.launch { _crime.value = repository.getCrime(crimeId) }
    }

    fun updateCrime(onUpdate: (Crime) -> Crime) {
        _crime.update { oldCrime -> oldCrime?.let(onUpdate) }
    }

    fun saveCrime() {
        _crime.value?.let { repository.updateCrime(it) }
    }

    fun deleteCrime() {
        _crime.value?.let { repository.deleteCrime(it) }
    }

    override fun onCleared() {
        super.onCleared()
        saveCrime()
    }
}

class CrimeDetailViewModelFactory(private val crimeId: UUID) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CrimeDetailViewModel(crimeId) as T
    }
}
