package com.example.criminalintent.ui.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.criminalintent.R
import com.example.criminalintent.data.Crime
import com.example.criminalintent.databinding.FragmentCrimeDetailBinding
import com.example.criminalintent.util.PictureUtils
import com.example.criminalintent.util.toDisplayString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * Detail screen: edit title, date (DatePickerFragment), solved flag, photo (camera),
 * suspect (Contacts picker) and share a report (ACTION_SEND).
 */
class CrimeDetailFragment : Fragment() {

    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) { "Cannot access binding because it is null. Is the view visible?" }

    private val crimeId: UUID by lazy {
        UUID.fromString(requireArguments().getString(ARG_CRIME_ID))
    }
    private val viewModel: CrimeDetailViewModel by viewModels { CrimeDetailViewModelFactory(crimeId) }

    // Name of the photo the camera is currently writing. Saved in onSaveInstanceState
    // so a rotation while the camera app is open doesn't lose it.
    private var photoName: String? = null

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { didTakePhoto ->
        val newName = photoName
        if (newName != null) {
            if (didTakePhoto) {
                val oldName = viewModel.crime.value?.photoFileName
                viewModel.updateCrime { it.copy(photoFileName = newName) }
                if (oldName != null && oldName != newName) photoFile(oldName).delete()
            } else {
                photoFile(newName).delete() // user cancelled; remove any empty file
            }
        }
    }

    private val pickContact = registerForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
        uri?.let { readSuspectName(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoName = savedInstanceState?.getString(STATE_PHOTO_NAME)

        setFragmentResultListener(DatePickerFragment.REQUEST_KEY_DATE) { _, bundle ->
            val newDate = Date(bundle.getLong(DatePickerFragment.BUNDLE_KEY_DATE))
            viewModel.updateCrime { it.copy(date = newDate) }
        }
        setFragmentResultListener(ConfirmDeleteDialogFragment.REQUEST_KEY_CONFIRM_DELETE) { _, _ ->
            deleteCrime()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                viewModel.updateCrime { it.copy(title = text.toString()) }
            }
            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateCrime { it.copy(isSolved = isChecked) }
            }
            crimeDate.setOnClickListener {
                viewModel.crime.value?.let { crime ->
                    DatePickerFragment.newInstance(crime.date)
                        .show(parentFragmentManager, DatePickerFragment.TAG)
                }
            }
            crimeCamera.setOnClickListener { launchCamera() }
            crimeSuspect.setOnClickListener {
                try {
                    pickContact.launch(null)
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.no_contacts_app)
                }
            }
            crimeReport.setOnClickListener {
                viewModel.crime.value?.let { sendReport(it) }
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_crime_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                R.id.delete_crime -> {
                    ConfirmDeleteDialogFragment()
                        .show(parentFragmentManager, ConfirmDeleteDialogFragment.TAG)
                    true
                }
                android.R.id.home -> {
                    parentFragmentManager.popBackStack()
                    true
                }
                else -> false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.crime.collect { crime -> crime?.let { updateUi(it) } }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PHOTO_NAME, photoName)
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveCrime()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(crime: Crime) {
        binding.apply {
            // Only touch the EditText if the text really differs, otherwise the cursor jumps.
            if (crimeTitle.text.toString() != crime.title) {
                crimeTitle.setText(crime.title)
            }
            crimeDate.text = crime.date.toDisplayString()
            if (crimeSolved.isChecked != crime.isSolved) {
                crimeSolved.isChecked = crime.isSolved
            }
            crimeSuspect.text = crime.suspect.ifBlank { getString(R.string.crime_suspect_text) }
        }
        updatePhoto(crime.photoFileName)
    }

    private fun updatePhoto(photoFileName: String?) {
        val photoView = binding.crimePhoto
        if (photoView.tag == photoFileName) return

        val file = photoFileName?.let { photoFile(it) }
        if (file != null && file.exists()) {
            photoView.tag = photoFileName
            val sizePx = resources.getDimensionPixelSize(R.dimen.photo_size)
            // Decode off the main thread so big camera images never cause jank.
            viewLifecycleOwner.lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    PictureUtils.getScaledBitmap(file.path, sizePx, sizePx)
                }
                photoView.setImageBitmap(bitmap)
                photoView.contentDescription = getString(R.string.crime_photo_image_description)
            }
        } else {
            photoView.tag = null
            photoView.setImageDrawable(null)
            photoView.contentDescription = getString(R.string.crime_photo_no_image_description)
        }
    }

    private fun launchCamera() {
        val newName = "IMG_${System.currentTimeMillis()}.jpg"
        photoName = newName
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile(newName)
        )
        try {
            takePhoto.launch(photoUri)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_camera_app)
        }
    }

    private fun photoFile(name: String): File =
        File(requireContext().applicationContext.filesDir, name)

    /** Reads the picked contact's display name (no extra permission needed for a picked contact). */
    private fun readSuspectName(contactUri: Uri) {
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        requireActivity().contentResolver.query(contactUri, queryFields, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val suspect = cursor.getString(0) ?: ""
                viewModel.updateCrime { it.copy(suspect = suspect) }
            }
        }
    }

    /** Implicit intent: hand a text report to any app that can share text. */
    private fun sendReport(crime: Crime) {
        val reportIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
        }
        try {
            startActivity(Intent.createChooser(reportIntent, getString(R.string.send_report)))
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_share_app)
        }
    }

    private fun getCrimeReport(crime: Crime): String {
        val solved = getString(
            if (crime.isSolved) R.string.crime_report_solved else R.string.crime_report_unsolved
        )
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        val title = crime.title.ifBlank { getString(R.string.untitled_crime) }
        return getString(R.string.crime_report, title, crime.date.toDisplayString(), solved, suspect)
    }

    private fun deleteCrime() {
        viewModel.crime.value?.photoFileName?.let { photoFile(it).delete() }
        viewModel.deleteCrime()
        parentFragmentManager.popBackStack()
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ARG_CRIME_ID = "crime_id"
        private const val STATE_PHOTO_NAME = "photo_name"

        fun newInstance(crimeId: UUID): CrimeDetailFragment = CrimeDetailFragment().apply {
            arguments = bundleOf(ARG_CRIME_ID to crimeId.toString())
        }
    }
}
