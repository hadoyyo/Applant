package pl.example.applant

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddRoomActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var imageView: ImageView
    private lateinit var addImageButton: Button
    private lateinit var saveButton: Button

    private var selectedImageUri: Uri? = null
    private lateinit var roomStorage: RoomStorage
    private var photoFile: File? = null
    private var alertDialog: AlertDialog? = null // Referencja do dialogu

    // Lista domyślnych zdjęć z zasobów
    private val defaultImages = listOf(
        R.drawable.room_1,
        R.drawable.room_2,
        R.drawable.room_3,
        R.drawable.room_4,
        R.drawable.room_5,
        R.drawable.room_6,
        R.drawable.room_7,
        R.drawable.room_8,
        R.drawable.room_9
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_room)

        roomStorage = RoomStorage(this)

        nameEditText = findViewById(R.id.edit_name)
        descriptionEditText = findViewById(R.id.edit_description)
        imageView = findViewById(R.id.image_preview)
        addImageButton = findViewById(R.id.button_add_image)
        saveButton = findViewById(R.id.button_save)

        addImageButton.setOnClickListener {
            showImageSourceDialog()
        }

        saveButton.setOnClickListener {
            saveRoom()
        }

        val darkModeEnabled = isSystemDarkModeEnabled(this)

        // Ustaw kolor paska stanu w zależności od trybu
        if (darkModeEnabled) {
            changeStatusBarColor("#18191B") // Ciemny tryb
        } else {
            changeStatusBarColor("#f9f9f9") // Jasny tryb
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.choose_default_image),
            getString(R.string.cancel)
        )
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.choose_image_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> openGallery()
                    2 -> chooseDefaultImage()
                    3 -> {}
                }
            }
            .show()
    }

    private fun chooseDefaultImage() {
        // Tworzenie RecyclerView
        val recyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@AddRoomActivity, 3) // 3 kolumny
            adapter = ImageAdapter(defaultImages) { selectedImageResId ->

                imageView.setImageResource(selectedImageResId)
                imageView.tag = selectedImageResId
                selectedImageUri = null

                // Zamknij dialog po wybraniu zdjęcia
                alertDialog?.dismiss()
            }
        }


        alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.choose_default_image_title))
            .setView(recyclerView)
            .setNegativeButton(R.string.cancel, null)
            .create()

        alertDialog?.show()

        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
        alertDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            photoFile = createImageFile()
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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

    private fun changeStatusBarColor(color: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = android.graphics.Color.parseColor(color)

            // Ustawienie koloru ikon na pasku stanu
            if (color == "#f9f9f9") { // Jasny tryb
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else { // Ciemny tryb
                window.decorView.systemUiVisibility = 0
            }
        }
    }

    private fun isSystemDarkModeEnabled(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Sprawdź, czy tryb ciemny jest włączony
        val darkModeEnabled = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        // Ustaw kolor paska stanu w zależności od trybu
        if (darkModeEnabled) {
            changeStatusBarColor("#18191B") // Ciemny tryb
        } else {
            changeStatusBarColor("#f9f9f9") // Jasny tryb
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    selectedImageUri = data?.data
                    selectedImageUri?.let { uri ->
                        val inputStream = contentResolver.openInputStream(uri)
                        val file = File(filesDir, "room_image_${System.currentTimeMillis()}.jpg")
                        val outputStream = FileOutputStream(file)
                        inputStream?.copyTo(outputStream)
                        outputStream.close()
                        inputStream?.close()
                        imageView.setImageURI(Uri.fromFile(file))
                        selectedImageUri = Uri.fromFile(file)
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    photoFile?.let { file ->
                        selectedImageUri = Uri.fromFile(file)
                        imageView.setImageURI(selectedImageUri)
                    }
                }
            }
        }
    }

    private fun saveRoom() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.name_required), Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdź, czy wybrano domyślne zdjęcie
        val defaultImageResId = if (selectedImageUri == null) {
            // Jeśli wybrano domyślne zdjęcie, pobierz jego ID
            val drawableId = imageView.tag as? Int
            drawableId
        } else {
            null
        }

        val newRoom = Room(
            name = name,
            description = description,
            imageUri = selectedImageUri?.toString(),
            defaultImageResId = defaultImageResId,
            isDefaultImage = defaultImageResId != null
        )

        val rooms = roomStorage.getRooms().toMutableList()
        rooms.add(newRoom)
        roomStorage.saveRooms(rooms)

        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
    }
}