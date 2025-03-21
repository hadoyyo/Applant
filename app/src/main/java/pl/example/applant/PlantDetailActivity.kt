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
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class PlantDetailActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var imageView: ImageView
    private lateinit var changeImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var spinnerRoom: Spinner
    private lateinit var wateringIntervalEditText: EditText
    private lateinit var buttonWater: Button
    private lateinit var buttonFertilize: Button
    private lateinit var buttonRepot: Button
    private lateinit var buttonDelete: Button
    private lateinit var recyclerWatering: RecyclerView
    private lateinit var recyclerFertilizing: RecyclerView
    private lateinit var recyclerRepotting: RecyclerView
    private lateinit var noteEditText: EditText

    private lateinit var radioGroupEvents: RadioGroup
    private lateinit var radioWatering: RadioButton
    private lateinit var radioFertilizing: RadioButton
    private lateinit var radioRepotting: RadioButton

    private lateinit var textEmptyWatering: TextView
    private lateinit var textEmptyFertilizing: TextView
    private lateinit var textEmptyRepotting: TextView

    private var selectedImageUri: Uri? = null
    private lateinit var plantStorage: PlantStorage
    private lateinit var roomStorage: RoomStorage
    private lateinit var plant: Plant

    private lateinit var wateringAdapter: EventAdapter
    private lateinit var fertilizingAdapter: EventAdapter
    private lateinit var repottingAdapter: EventAdapter

    private var alertDialog: AlertDialog? = null

    private lateinit var suggestNameButton: ImageButton

    private var photoFile: File? = null
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
        setContentView(R.layout.activity_plant_detail)

        plantStorage = PlantStorage(this)
        roomStorage = RoomStorage(this)

        nameEditText = findViewById(R.id.edit_name)
        descriptionEditText = findViewById(R.id.edit_description)
        imageView = findViewById(R.id.image_preview)
        changeImageButton = findViewById(R.id.button_edit_image)
        saveButton = findViewById(R.id.button_save)
        spinnerRoom = findViewById(R.id.spinner_room)
        wateringIntervalEditText = findViewById(R.id.edit_watering_interval_days)
        buttonWater = findViewById(R.id.button_water)
        buttonFertilize = findViewById(R.id.button_fertilize)
        buttonRepot = findViewById(R.id.button_repot)
        buttonDelete = findViewById(R.id.button_delete)
        recyclerWatering = findViewById(R.id.recycler_watering)
        recyclerFertilizing = findViewById(R.id.recycler_fertilizing)
        recyclerRepotting = findViewById(R.id.recycler_repotting)
        noteEditText = findViewById(R.id.edit_note)

        radioGroupEvents = findViewById(R.id.radio_group_events)
        radioWatering = findViewById(R.id.radio_watering)
        radioFertilizing = findViewById(R.id.radio_fertilizing)
        radioRepotting = findViewById(R.id.radio_repotting)

        suggestNameButton = findViewById(R.id.button_suggest_name)

        textEmptyWatering = findViewById(R.id.text_empty_watering)
        textEmptyFertilizing = findViewById(R.id.text_empty_fertilizing)
        textEmptyRepotting = findViewById(R.id.text_empty_repotting)

        wateringAdapter = EventAdapter(mutableMapOf(), this)
        fertilizingAdapter = EventAdapter(mutableMapOf(), this)
        repottingAdapter = EventAdapter(mutableMapOf(), this)

        recyclerWatering.layoutManager = LinearLayoutManager(this)
        recyclerFertilizing.layoutManager = LinearLayoutManager(this)
        recyclerRepotting.layoutManager = LinearLayoutManager(this)

        recyclerWatering.adapter = wateringAdapter
        recyclerFertilizing.adapter = fertilizingAdapter
        recyclerRepotting.adapter = repottingAdapter


        val json = intent.getStringExtra("plant")
        if (json != null) {
            plant = Gson().fromJson(json, Plant::class.java)
            loadPlantData()
        }

        changeImageButton.setOnClickListener {
            showImageSourceDialog()
        }

        saveButton.setOnClickListener {
            saveChanges()
        }

        buttonWater.setOnClickListener {
            addEvent(plant.wateringDates, wateringAdapter, getString(R.string.watering_added), textEmptyWatering)
        }

        buttonFertilize.setOnClickListener {
            addEvent(plant.fertilizingDates, fertilizingAdapter, getString(R.string.fertilizing_added), textEmptyFertilizing)
        }

        buttonRepot.setOnClickListener {
            addEvent(plant.repottingDates, repottingAdapter, getString(R.string.repotting_added), textEmptyRepotting)
        }

        buttonDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        suggestNameButton.setOnClickListener {
            if (selectedImageUri == null) {
                Toast.makeText(this, R.string.add_photo_for_identification, Toast.LENGTH_SHORT).show()
            } else {
                identifyPlantFromImage()
            }
        }

        // Ustaw domyślny wybór na podlewania
        radioWatering.isChecked = true
        showSection(recyclerWatering, buttonWater, textEmptyWatering)

        // Obsługa zmiany wyboru w RadioGroup
        radioGroupEvents.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_watering -> {
                    showSection(recyclerWatering, buttonWater, textEmptyWatering)
                    hideSection(recyclerFertilizing, buttonFertilize, textEmptyFertilizing)
                    hideSection(recyclerRepotting, buttonRepot, textEmptyRepotting)
                }
                R.id.radio_fertilizing -> {
                    showSection(recyclerFertilizing, buttonFertilize, textEmptyFertilizing)
                    hideSection(recyclerWatering, buttonWater, textEmptyWatering)
                    hideSection(recyclerRepotting, buttonRepot, textEmptyRepotting)
                }
                R.id.radio_repotting -> {
                    showSection(recyclerRepotting, buttonRepot, textEmptyRepotting)
                    hideSection(recyclerWatering, buttonWater, textEmptyWatering)
                    hideSection(recyclerFertilizing, buttonFertilize, textEmptyFertilizing)
                }
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

    private fun showSection(recyclerView: RecyclerView, button: Button, emptyMessage: TextView) {
        recyclerView.visibility = View.VISIBLE
        button.visibility = View.VISIBLE
        if (recyclerView.adapter?.itemCount == 0) {
            emptyMessage.visibility = View.VISIBLE
        } else {
            emptyMessage.visibility = View.GONE
        }
    }

    private fun hideSection(recyclerView: RecyclerView, button: Button, emptyMessage: TextView) {
        recyclerView.visibility = View.GONE
        button.visibility = View.GONE
        emptyMessage.visibility = View.GONE
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

    private fun loadPlantData() {
        nameEditText.setText(plant.name)
        descriptionEditText.setText(plant.description)
        wateringIntervalEditText.setText(
            if (plant.wateringIntervalDays > 0) plant.wateringIntervalDays.toString() else ""
        )
        noteEditText.setText(plant.note) // Załaduj notatkę

        if (!plant.imageUri.isNullOrEmpty()) {
            selectedImageUri = Uri.parse(plant.imageUri)
        }

        // Obsługa domyślnych zdjęć
        if (plant.isDefaultImage) {
            plant.defaultImageResId?.let { resId ->
                imageView.setImageResource(resId)
            }
        } else if (!plant.imageUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(plant.imageUri)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.plant_1)
        }

        val rooms = roomStorage.getRooms()
        val roomNames = rooms.map { it.name }.toMutableList()
        roomNames.add(0, getString(R.string.no_room_assigned_option))

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roomNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRoom.adapter = adapter

        val roomIndex = if (plant.room != null) {
            rooms.indexOfFirst { it.id == plant.room!!.id } + 1
        } else {
            0
        }
        spinnerRoom.setSelection(roomIndex)

        wateringAdapter.updateData(processDates(plant.wateringDates))
        fertilizingAdapter.updateData(processDates(plant.fertilizingDates))
        repottingAdapter.updateData(processDates(plant.repottingDates))
    }

    private fun saveChanges() {
        val updatedName = nameEditText.text.toString().trim()
        if (updatedName.isEmpty()) {
            Toast.makeText(this, getString(R.string.name_required), Toast.LENGTH_SHORT).show()
            return
        }

        val updatedDescription = descriptionEditText.text.toString().trim()
        val updatedWateringInterval = wateringIntervalEditText.text.toString().toIntOrNull() ?: 0
        val updatedNote = noteEditText.text.toString().trim() // Pobierz notatkę

        val selectedRoomName = spinnerRoom.selectedItem.toString()
        val updatedRoom = if (selectedRoomName == "Brak przypisanego pokoju") {
            null
        } else {
            roomStorage.getRoomByName(selectedRoomName)
        }

        plant.name = updatedName
        plant.description = updatedDescription
        plant.wateringIntervalDays = updatedWateringInterval
        plant.room = updatedRoom
        plant.note = updatedNote // Zaktualizuj notatkę

        if (selectedImageUri != null) {
            plant.imageUri = selectedImageUri?.toString()
            plant.defaultImageResId = null
            plant.isDefaultImage = false
        }

        val plants = plantStorage.getPlants().toMutableList()
        val index = plants.indexOfFirst { it.id == plant.id }

        if (index != -1) {
            plants[index] = plant
        } else {
            plants.add(plant)
        }

        plantStorage.savePlants(plants)
        plantStorage.scheduleWateringNotification(this, plant)

        val resultIntent = Intent()
        resultIntent.putExtra("updated_plant", Gson().toJson(plant))
        setResult(Activity.RESULT_OK, resultIntent)

        finish()
    }

    private fun showDeleteConfirmationDialog() {
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.confirmation))
            .setMessage(getString(R.string.remove_plant_question))
            .setPositiveButton(getString(R.string.yes)) { _, _ -> deletePlant() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
    }

    private fun deletePlant() {
        val plants = plantStorage.getPlants().toMutableList()
        val index = plants.indexOfFirst { it.id == plant.id }

        if (index != -1) {
            plants.removeAt(index)
            plantStorage.savePlants(plants)

            Toast.makeText(this, getString(R.string.plant_removed_info), Toast.LENGTH_SHORT).show()

            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)

            finish()
        } else {
            Toast.makeText(this, getString(R.string.no_plant_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun processDates(dates: List<String>): Map<Int, Map<String, List<Int>>> {
        val result = mutableMapOf<Int, MutableMap<String, MutableList<Int>>>()

        for (date in dates) {
            val parts = date.split("-")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()

                val monthName = when (month) {
                    1 -> getString(R.string.january)
                    2 -> getString(R.string.february)
                    3 -> getString(R.string.march)
                    4 -> getString(R.string.april)
                    5 -> getString(R.string.may)
                    6 -> getString(R.string.june)
                    7 -> getString(R.string.july)
                    8 -> getString(R.string.august)
                    9 -> getString(R.string.september)
                    10 -> getString(R.string.october)
                    11 -> getString(R.string.november)
                    12 -> getString(R.string.december)
                    else -> ""
                }

                if (monthName.isNotEmpty()) {
                    val yearMap = result.getOrPut(year) { mutableMapOf() }
                    val dayList = yearMap.getOrPut(monthName) { mutableListOf() }
                    dayList.add(day)
                }
            }
        }

        // Sortowanie miesięcy w każdej mapie roku
        for ((year, monthsMap) in result) {
            val sortedMonthsMap = monthsMap.toSortedMap(compareBy {
                when (it) {
                    getString(R.string.january) -> 1
                    getString(R.string.february) -> 2
                    getString(R.string.march) -> 3
                    getString(R.string.april) -> 4
                    getString(R.string.may) -> 5
                    getString(R.string.june) -> 6
                    getString(R.string.july) -> 7
                    getString(R.string.august) -> 8
                    getString(R.string.september) -> 9
                    getString(R.string.october) -> 10
                    getString(R.string.november) -> 11
                    getString(R.string.december) -> 12
                    else -> 0
                }
            })
            result[year] = sortedMonthsMap
        }

        // Sortowanie dni w każdym miesiącu
        for ((year, monthsMap) in result) {
            for ((month, days) in monthsMap) {
                days.sort()
            }
        }

        return result
    }

    private fun toggleSection(
        recyclerView: RecyclerView,
        button: Button,
        toggleButton: Button,
        showText: String,
        hideText: String,
        emptyMessage: TextView
    ) {
        if (recyclerView.visibility == View.GONE) {
            recyclerView.visibility = View.VISIBLE
            button.visibility = View.VISIBLE
            toggleButton.text = hideText

            if (recyclerView.adapter?.itemCount == 0) {
                emptyMessage.visibility = View.VISIBLE
            } else {
                emptyMessage.visibility = View.GONE
            }
        } else {
            recyclerView.visibility = View.GONE
            button.visibility = View.GONE
            toggleButton.text = showText
            emptyMessage.visibility = View.GONE
        }
    }

    private fun addEvent(eventList: MutableList<String>, adapter: EventAdapter, message: String, emptyMessage: TextView) {
        val datePicker = DatePicker(this)
        val currentDate = Calendar.getInstance()

        // Tworzenie niestandardowego okna dialogowego
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(datePicker)
            .setPositiveButton("OK") { _, _ ->
                val selectedDate = Calendar.getInstance().apply {
                    set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Resetowanie czasu w currentDate do północy
                currentDate.set(Calendar.HOUR_OF_DAY, 0)
                currentDate.set(Calendar.MINUTE, 0)
                currentDate.set(Calendar.SECOND, 0)
                currentDate.set(Calendar.MILLISECOND, 0)

                // Sprawdź, czy wybrana data jest w przyszłości (ale pozwól na datę dzisiejszą)
                if (selectedDate.after(currentDate)) {
                    Toast.makeText(this, getString(R.string.date_from_future), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val date = "${datePicker.dayOfMonth}-${datePicker.month + 1}-${datePicker.year}"
                eventList.add(date)
                adapter.updateData(processDates(eventList))

                // Ukryj komunikat, jeśli są dane
                if (eventList.isNotEmpty()) {
                    emptyMessage.visibility = View.GONE
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)

        // Pokaż okno dialogowe
        .show()

        // Zmiana koloru przycisków
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))

    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
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

    // Wybór domyślnego zdjęcia
    private fun chooseDefaultImage() {
        // Tworzenie RecyclerView
        val recyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@PlantDetailActivity, 3) // 3 kolumny
            adapter = ImageAdapter(defaultImages) { selectedImageResId ->
                // Po wybraniu zdjęcia ustaw je w ImageView
                imageView.setImageResource(selectedImageResId)
                imageView.tag = selectedImageResId // Zapisz ID zasobu jako tag

                // Aktualizuj pola w obiekcie Plant
                plant.defaultImageResId = selectedImageResId
                plant.isDefaultImage = true
                plant.imageUri = null // Resetowanie URI, ponieważ używamy domyślnego zdjęcia

                selectedImageUri = null

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

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
    }
}