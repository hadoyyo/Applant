package pl.example.applant

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import android.text.SpannableStringBuilder
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DiagnoseFragment : Fragment() {

    private lateinit var imagePreview: ImageView
    private lateinit var placeholderIcon_inner: ImageView
    private lateinit var placeholderIcon_outer: ImageView
    private lateinit var addImageButton: Button
    private lateinit var identifyButton: Button
    private lateinit var retryButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultText: TextView
    private lateinit var gestureDetector: GestureDetector

    private var selectedImageUri: Uri? = null
    private var photoFile: File? = null
    private lateinit var plantIdService: PlantIdService

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
        private const val STATE_IMAGE_URI = "image_uri"
        private const val STATE_RESULT_TEXT = "result_text"
        private const val STATE_SHOWING_RESULT = "showing_result"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_diagnose, container, false)

        gestureDetector = GestureDetector(requireContext(), SwipeGestureListener())

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        return view
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imagePreview = view.findViewById(R.id.image_preview)
        placeholderIcon_inner = view.findViewById(R.id.placeholder_icon_inner)
        placeholderIcon_outer = view.findViewById(R.id.placeholder_icon_outer)
        addImageButton = view.findViewById(R.id.button_add_image)
        identifyButton = view.findViewById(R.id.button_identify)
        retryButton = view.findViewById(R.id.button_retry)
        progressBar = view.findViewById(R.id.progress_bar)
        resultText = view.findViewById(R.id.result_text)

        plantIdService = PlantIdService(requireContext())

        savedInstanceState?.let { restoreState(it) } ?: showPlaceholder()

        addImageButton.setOnClickListener { showImageSourceDialog() }
        identifyButton.setOnClickListener { processSelectedImage() }
        retryButton.setOnClickListener { resetDiagnosis() }

        view.findViewById<ScrollView>(R.id.scroll_view)?.setOnTouchListener(
            OnSwipeTouchListener(requireContext()) { direction ->
                (activity as? MainActivity)?.let { mainActivity ->
                    when (direction) {
                        "left" -> mainActivity.onSwipeLeft()
                        "right" -> mainActivity.onSwipeRight()
                    }
                }
                true
            }
        )
    }


    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100 // Minimalna odległość przesunięcia
        private val SWIPE_VELOCITY_THRESHOLD = 100 // Minimalna prędkość przesunięcia

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - (e1?.x ?: 0f)
            val diffY = e2.y - (e1?.y ?: 0f)

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        navigateToRooms()
                    } else {
                        navigateToOptions()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun navigateToRooms() {
        (activity as? MainActivity)?.apply {
            replaceFragment(Rooms(), R.id.rooms)
            binding.bottomNavigationView.selectedItemId = R.id.rooms
        }
    }

    private fun navigateToOptions() {
        (activity as? MainActivity)?.apply {
            replaceFragment(Options(), R.id.options)
            binding.bottomNavigationView.selectedItemId = R.id.options
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedImageUri?.let { uri ->
            outState.putString(STATE_IMAGE_URI, uri.toString())
        }
        outState.putCharSequence(STATE_RESULT_TEXT, resultText.text)
        outState.putBoolean(STATE_SHOWING_RESULT, resultText.visibility == View.VISIBLE)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        val imageUriString = savedInstanceState.getString(STATE_IMAGE_URI)
        imageUriString?.let { uriString ->
            selectedImageUri = Uri.parse(uriString)
            Glide.with(this)
                .load(selectedImageUri)
                .into(imagePreview)

            if (savedInstanceState.getBoolean(STATE_SHOWING_RESULT, false)) {
                resultText.text = savedInstanceState.getCharSequence(STATE_RESULT_TEXT)
                showResultState()
            } else {
                showImage()
            }
        } ?: showPlaceholder()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.cancel)
        )

        AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle(getString(R.string.choose_image_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> openGallery()
                    2 -> {}
                }
            }
            .show()
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            photoFile = createImageFile()
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> handleGalleryResult(data)
                REQUEST_IMAGE_CAPTURE -> handleCameraResult()
            }
        }
    }

    private fun handleGalleryResult(data: Intent?) {
        selectedImageUri = data?.data
        selectedImageUri?.let {
            Glide.with(this)
                .load(it)
                .into(imagePreview)
            showImage()
        }
    }

    private fun handleCameraResult() {
        photoFile?.let { file ->
            selectedImageUri = Uri.fromFile(file)
            Glide.with(this)
                .load(selectedImageUri)
                .into(imagePreview)
            showImage()
        }
    }

    private fun processSelectedImage() {
        selectedImageUri?.let { uri ->
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(
                    requireActivity().contentResolver,
                    uri
                )
                identifyPlantDisease(bitmap)
            } catch (e: Exception) {
                showError(getString(R.string.image_load_error))
            }
        }
    }

    private fun identifyPlantDisease(bitmap: Bitmap) {
        progressBar.visibility = View.VISIBLE
        identifyButton.visibility = View.GONE
        resultText.visibility = View.GONE

        plantIdService.assessPlantHealth(bitmap) { result ->
            requireActivity().runOnUiThread {
                resultText.text = result
                showResultState()
            }
        }
    }

    private fun showPlaceholder() {
        placeholderIcon_inner.visibility = View.VISIBLE
        placeholderIcon_outer.visibility = View.VISIBLE
        imagePreview.visibility = View.GONE
        addImageButton.visibility = View.VISIBLE
        identifyButton.visibility = View.GONE
        retryButton.visibility = View.GONE
        resultText.visibility = View.GONE
    }

    private fun showImage() {
        placeholderIcon_inner.visibility = View.GONE
        placeholderIcon_outer.visibility = View.GONE
        imagePreview.visibility = View.VISIBLE
        addImageButton.visibility = View.GONE
        identifyButton.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
        resultText.visibility = View.GONE
    }

    private fun showResultState() {
        placeholderIcon_inner.visibility = View.GONE
        placeholderIcon_outer.visibility = View.GONE
        imagePreview.visibility = View.VISIBLE
        addImageButton.visibility = View.GONE
        identifyButton.visibility = View.GONE
        retryButton.visibility = View.VISIBLE
        resultText.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    private fun resetDiagnosis() {
        selectedImageUri = null
        imagePreview.setImageURI(null)
        resultText.text = ""
        showPlaceholder()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}