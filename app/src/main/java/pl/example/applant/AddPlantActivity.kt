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
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.content.FileProvider
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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

import android.graphics.Bitmap
import android.widget.Toast

class AddPlantActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var imageView: ImageView
    private lateinit var addImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var spinnerRoom: Spinner
    private lateinit var wateringIntervalEditText: EditText

    private var selectedImageUri: Uri? = null
    private lateinit var plantStorage: PlantStorage
    private lateinit var roomStorage: RoomStorage
    private var photoFile: File? = null
    private var alertDialog: AlertDialog? = null

    private lateinit var suggestNameButton: ImageButton

    // Lista domyślnych zdjęć z zasobów
    private val defaultImages = listOf(
        R.drawable.plant_1,
        R.drawable.plant_2,
        R.drawable.plant_3,
        R.drawable.plant_4,
        R.drawable.plant_5,
        R.drawable.plant_6,
        R.drawable.plant_7,
        R.drawable.plant_8,
        R.drawable.plant_9,
        R.drawable.plant_10,
        R.drawable.plant_11,
        R.drawable.plant_12,
        R.drawable.plant_13,
        R.drawable.plant_14,
        R.drawable.plant_15
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_plant)

        plantStorage = PlantStorage(this)
        roomStorage = RoomStorage(this)

        nameEditText = findViewById(R.id.edit_name)
        descriptionEditText = findViewById(R.id.edit_description)
        imageView = findViewById(R.id.image_preview)
        addImageButton = findViewById(R.id.button_add_image)
        saveButton = findViewById(R.id.button_save)
        spinnerRoom = findViewById(R.id.spinner_room)
        wateringIntervalEditText = findViewById(R.id.edit_watering_interval_days)

        suggestNameButton = findViewById(R.id.button_suggest_name)


        // Inicjalizacja spinnera z pokojami
        val rooms = roomStorage.getRooms()
        val roomNames = rooms.map { it.name }.toMutableList()
        roomNames.add(0, getString(R.string.no_room_assigned_option))

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roomNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRoom.adapter = adapter

        // Obsługa przycisku dodania zdjęcia
        addImageButton.setOnClickListener {
            showImageSourceDialog()
        }

        // Obsługa przycisku zapisu
        saveButton.setOnClickListener {
            savePlant()
        }

        suggestNameButton.setOnClickListener {
            if (selectedImageUri == null) {
                Toast.makeText(this, R.string.add_photo_for_identification, Toast.LENGTH_SHORT).show()
            } else {
                identifyPlantFromImage()
            }
        }



        val darkModeEnabled = isSystemDarkModeEnabled(this)

        // Ustaw kolor paska stanu w zależności od trybu
        if (darkModeEnabled) {
            changeStatusBarColor("#18191B") // Ciemny tryb
        } else {
            changeStatusBarColor("#f9f9f9") // Jasny tryb
        }
    }

    // Wyświetlanie dialogu wyboru źródła zdjęcia
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
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


    // Wybór domyślnego zdjęcia
    private fun chooseDefaultImage() {
        // Tworzenie RecyclerView
        val recyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@AddPlantActivity, 3) // 3 kolumny
            adapter = ImageAdapter(defaultImages) { selectedImageResId ->
                // Po wybraniu zdjęcia ustaw je w ImageView
                imageView.setImageResource(selectedImageResId)
                imageView.tag = selectedImageResId // Zapisz ID zasobu jako tag
                selectedImageUri = null // Resetowanie URI, ponieważ używamy domyślnego zdjęcia

                // Zamknij dialog po wybraniu zdjęcia
                alertDialog?.dismiss()
            }
        }

        // Tworzenie i wyświetlenie dialogu z siatką zdjęć
        alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.choose_default_image_title))
            .setView(recyclerView)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        alertDialog?.show()

        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
        alertDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
    }

    // Wykonywanie zdjęcia
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

    // Tworzenie pliku dla zdjęcia
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    // Otwieranie galerii
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    // Obsługa wyniku z aktywności (zdjęcie lub galeria)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    selectedImageUri = data?.data
                    selectedImageUri?.let { uri ->
                        val inputStream = contentResolver.openInputStream(uri)
                        val file = File(filesDir, "plant_image_${System.currentTimeMillis()}.jpg")
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

    private fun identifyPlantFromImage() {
        // Sprawdź, czy urządzenie ma połączenie z internetem
        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
        val plantNetService = PlantNetService(this)

        suggestNameButton.isEnabled = false // Wyłącz przycisk podczas identyfikacji

        plantNetService.identifyPlant(bitmap) { plantName ->
            runOnUiThread {

                suggestNameButton.isEnabled = true // Włącz przycisk po zakończeniu

                if (plantName != null) {
                    nameEditText.setText(plantName) // Wpisz nazwę rośliny w pole
                } else {
                    Toast.makeText(this, R.string.plant_identification_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Zapisywanie rośliny
    private fun savePlant() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val wateringInterval = wateringIntervalEditText.text.toString().trim().toIntOrNull() ?: 0

        val selectedRoomName = spinnerRoom.selectedItem.toString()
        val room = if (selectedRoomName == "Nie przypisuj pokoju") {
            null
        } else {
            roomStorage.getRoomByName(selectedRoomName)
        }

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

        val newPlant = Plant(
            name = name,
            description = description.ifEmpty { null },
            wateringIntervalDays = wateringInterval,
            room = room,
            imageUri = selectedImageUri?.toString(),
            defaultImageResId = defaultImageResId,
            isDefaultImage = defaultImageResId != null // Ustaw, czy zdjęcie jest domyślne
        )

        val plants = plantStorage.getPlants().toMutableList()
        plants.add(newPlant)
        plantStorage.savePlants(plants)

        if (wateringInterval > 0) {
            plantStorage.scheduleWateringNotification(this, newPlant)
        }

        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
    }
}