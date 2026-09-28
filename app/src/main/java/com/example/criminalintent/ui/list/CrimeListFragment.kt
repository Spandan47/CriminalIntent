package com.example.criminalintent.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import com.example.criminalintent.R
import com.example.criminalintent.data.Crime
import com.example.criminalintent.databinding.FragmentCrimeListBinding
import com.example.criminalintent.ui.detail.CrimeDetailFragment
import kotlinx.coroutines.launch
import java.util.UUID

/** Master screen: a scrollable list of crimes plus an "add" action in the app bar. */
class CrimeListFragment : Fragment() {

    private var _binding: FragmentCrimeListBinding? = null
    private val binding
        get() = checkNotNull(_binding) { "Cannot access binding because it is null. Is the view visible?" }

    private val viewModel: CrimeListViewModel by viewModels()
    private val crimeAdapter = CrimeListAdapter { crimeId -> showCrimeDetail(crimeId) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        binding.crimeRecyclerView.adapter = crimeAdapter
        binding.emptyAddButton.setOnClickListener { addCrime() }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_crime_list, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                R.id.new_crime -> {
                    addCrime()
                    true
                }
                else -> false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.crimes.collect { crimes ->
                    crimeAdapter.submitList(crimes)
                    binding.emptyView.isVisible = crimes.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addCrime() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newCrime = Crime()
            viewModel.addCrime(newCrime)
            // Only navigate while the screen is resumed, so the transaction is always legal.
            viewLifecycleOwner.lifecycle.withResumed { showCrimeDetail(newCrime.id) }
        }
    }

    private fun showCrimeDetail(crimeId: UUID) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, CrimeDetailFragment.newInstance(crimeId))
            .addToBackStack(null)
            .commit()
    }
}
