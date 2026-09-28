package com.example.criminalintent.ui.list

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.criminalintent.R
import com.example.criminalintent.data.Crime
import com.example.criminalintent.databinding.ListItemCrimeBinding
import com.example.criminalintent.util.toDisplayString
import java.util.UUID

class CrimeHolder(
    private val binding: ListItemCrimeBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(crime: Crime, onCrimeClicked: (UUID) -> Unit) {
        val context = binding.root.context
        binding.crimeTitle.text = crime.title.ifBlank { context.getString(R.string.untitled_crime) }
        binding.crimeDate.text = crime.date.toDisplayString()

        // Solved crimes get a check icon and a struck-through title.
        binding.crimeSolved.isVisible = crime.isSolved
        binding.crimeTitle.paintFlags = if (crime.isSolved) {
            binding.crimeTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.crimeTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        binding.root.setOnClickListener { onCrimeClicked(crime.id) }
    }
}

object CrimeDiffCallback : DiffUtil.ItemCallback<Crime>() {
    override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean = oldItem == newItem
}

class CrimeListAdapter(
    private val onCrimeClicked: (UUID) -> Unit
) : ListAdapter<Crime, CrimeHolder>(CrimeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val binding = ListItemCrimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CrimeHolder(binding)
    }

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        holder.bind(getItem(position), onCrimeClicked)
    }
}
