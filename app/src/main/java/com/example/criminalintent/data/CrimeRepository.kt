package com.example.criminalintent.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Single access point to the Room database. Updates and deletes run on an
 * application-lifetime scope so they still finish if the screen that
 * triggered them is closed a moment later.
 */
class CrimeRepository private constructor(
    context: Context,
    private val scope: CoroutineScope
) {
    private val database: CrimeDatabase = Room
        .databaseBuilder(context.applicationContext, CrimeDatabase::class.java, DATABASE_NAME)
        .build()

    private val dao = database.crimeDao()

    fun getCrimes(): Flow<List<Crime>> = dao.getCrimes()

    suspend fun getCrime(id: UUID): Crime? = dao.getCrime(id)

    suspend fun addCrime(crime: Crime) = dao.addCrime(crime)

    fun updateCrime(crime: Crime) {
        scope.launch { dao.updateCrime(crime) }
    }

    fun deleteCrime(crime: Crime) {
        scope.launch { dao.deleteCrime(crime) }
    }

    companion object {
        private const val DATABASE_NAME = "crime-database"
        private var INSTANCE: CrimeRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context, CoroutineScope(SupervisorJob() + Dispatchers.IO))
            }
        }

        fun get(): CrimeRepository =
            INSTANCE ?: throw IllegalStateException("CrimeRepository must be initialized")
    }
}
