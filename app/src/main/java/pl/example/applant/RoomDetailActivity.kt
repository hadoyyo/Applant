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

class RoomDetailActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var addPlantButton: Button
    private lateinit var changeImageButton: Button
    private lateinit var recyclerViewPlants: RecyclerView
    private lateinit var buttonDelete: Button
    private lateinit var imageView: ImageView
    private lateinit var textEmptyPlants: TextView
    private lateinit var buttonWaterAll: Button // Dodany przycisk

    private var selectedImageUri: Uri? = null
    private lateinit var roomStorage: RoomStorage
    private lateinit var plantStorage: PlantStorage
    private lateinit var room: Room
    private lateinit var roomPlantsAdapter: RoomPlantsAdapter

    private var alertDialog: AlertDialog? = null

    private var photoFile: File? = null

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
        setContentView(R.layout.activity_room_detail)

        roomStorage = RoomStorage(this)
        plantStorage = PlantStorage(this)

        nameEditText = findViewById(R.id.edit_name)
        descriptionEditText = findViewById(R.id.edit_description)
        saveButton = findViewById(R.id.button_save)
        addPlantButton = findViewById(R.id.button_add_plant)
        recyclerViewPlants = findViewById(R.id.recycler_view_plants)
        changeImageButton = findViewById(R.id.button_edit_image)
        buttonDelete = findViewById(R.id.button_delete)
        imageView = findViewById(R.id.image_preview)
        textEmptyPlants = findViewById(R.id.text_empty_plants)
        buttonWaterAll = findViewById(R.id.button_water_all) // Inicjalizacja przycisku

        recyclerViewPlants.layoutManager = LinearLayoutManager(this)
        roomPlantsAdapter = RoomPlantsAdapter(mutableListOf(), this::onRemoveAssignmentClicked)
        recyclerViewPlants.adapter = roomPlantsAdapter

        val json = intent.getStringExtra("room")
        if (json != null) {
            room = Gson().fromJson(json, Room::class.java)
            loadRoomData()
        }

        saveButton.setOnClickListener {
            saveChanges()
        }

        addPlantButton.setOnClickListener {
            showUnassignedPlantsDialog()
        }

        changeImageButton.setOnClickListener {
            showImageSourceDialog()
        }

        buttonDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        buttonWaterAll.setOnClickListener {
            waterAllPlantsInRoom() // Obsługa kliknięcia przycisku
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

    private fun loadRoomData() {
        nameEditText.setText(room.name)
        descriptionEditText.setText(room.description)

        val plants = plantStorage.getPlants().filter { it.room?.id == room.id }
        roomPlantsAdapter.updatePlants(plants)

        if (plants.isEmpty()) {
            textEmptyPlants.visibility = TextView.VISIBLE
            recyclerViewPlants.visibility = RecyclerView.GONE
            buttonWaterAll.visibility = View.GONE // Ukryj przycisk, jeśli nie ma roślin
        } else {
            textEmptyPlants.visibility = TextView.GONE
            recyclerViewPlants.visibility = RecyclerView.VISIBLE
            buttonWaterAll.visibility = View.VISIBLE // Pokaż przycisk, jeśli są rośliny
        }

        // Obsługa domyślnych zdjęć
        if (room.isDefaultImage) {
            room.defaultImageResId?.let { resId ->
                imageView.setImageResource(resId)
            }
        } else if (!room.imageUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(room.imageUri)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.room_1)
        }
    }

    private fun saveChanges() {
        val updatedName = nameEditText.text.toString().trim()
        val updatedDescription = descriptionEditText.text.toString().trim()

        if (updatedName.isEmpty()) {
            Toast.makeText(this, getString(R.string.name_required), Toast.LENGTH_SHORT).show()
            return
        }

        room.name = updatedName
        room.description = updatedDescription

        if (selectedImageUri != null) {
            room.imageUri = selectedImageUri?.toString()
            room.defaultImageResId = null
            room.isDefaultImage = false
        }

        val plants = plantStorage.getPlants().toMutableList()
        plants.forEach { plant ->
            if (plant.room?.id == room.id) {
                plant.room = room
            }
        }
        plantStorage.savePlants(plants)

        val rooms = roomStorage.getRooms().toMutableList()
        val index = rooms.indexOfFirst { it.id == room.id }

        if (index != -1) {
            rooms[index] = room
        } else {
            rooms.add(room)
        }
        roomStorage.saveRooms(rooms)

        val resultIntent = Intent()
        resultIntent.putExtra("updated_room", Gson().toJson(room))
        setResult(Activity.RESULT_OK, resultIntent)

        finish()
    }

    private fun showDeleteConfirmationDialog() {
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.confirmation))
            .setMessage(getString(R.string.remove_room_question))
            .setPositiveButton(getString(R.string.yes)) { _, _ -> deleteRoom() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
    }

    private fun deleteRoom() {
        val rooms = roomStorage.getRooms().toMutableList()
        val index = rooms.indexOfFirst { it.id == room.id }

        if (index != -1) {
            roomStorage.deleteRoom(room.id)

            Toast.makeText(this, getString(R.string.room_removed_info), Toast.LENGTH_SHORT).show()

            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)

            finish()
        } else {
            Toast.makeText(this, getString(R.string.no_room_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun onRemoveAssignmentClicked(plant: Plant) {
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.confirmation))
            .setMessage(getString(R.string.confirm_unassign_plant, plant.name))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                plant.room = null
                plantStorage.updatePlant(plant)

                val plants = plantStorage.getPlants().filter { it.room?.id == room.id }
                roomPlantsAdapter.updatePlants(plants)

                if (plants.isEmpty()) {
                    textEmptyPlants.visibility = TextView.VISIBLE
                    recyclerViewPlants.visibility = RecyclerView.GONE
                    buttonWaterAll.visibility = View.GONE // Ukryj przycisk, jeśli nie ma roślin
                } else {
                    textEmptyPlants.visibility = TextView.GONE
                    recyclerViewPlants.visibility = RecyclerView.VISIBLE
                }

                Toast.makeText(this, getString(R.string.plant_assign_removed), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
    }

    private fun showUnassignedPlantsDialog() {
        // Filtruj tylko rośliny, które nie są przypisane do żadnego pokoju
        val unassignedPlants = plantStorage.getPlants().filter { plant ->
            plant.room == null // Tylko rośliny bez przypisanego pokoju
        }

        if (unassignedPlants.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_plants_to_assign), Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = object : ArrayAdapter<Plant>(this, R.layout.item_plant_preview, unassignedPlants) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_plant_preview, parent, false)
                val plant = unassignedPlants[position]

                val textName = view.findViewById<TextView>(R.id.text_name)
                textName.text = plant.name

                val textDescription = view.findViewById<TextView>(R.id.text_description)
                textDescription.text = plant.description

                val textRoom = view.findViewById<TextView>(R.id.text_room)
                textRoom.text = plant.room?.name ?: getString(R.string.no_room_assigned)

                val textWateringInterval = view.findViewById<TextView>(R.id.text_watering_interval)
                if (plant.wateringIntervalDays > 0) {
                    if (plant.wateringIntervalDays == 1) {
                        textWateringInterval.text = getString(R.string.watering_every_day)
                    } else {
                        textWateringInterval.text = getString(R.string.watering_every_x_days, plant.wateringIntervalDays)
                    }
                    textWateringInterval.visibility = View.VISIBLE
                } else {
                    textWateringInterval.visibility = View.GONE
                }

                val imagePlant = view.findViewById<ImageView>(R.id.image_plant)
                if (plant.isDefaultImage) {
                    plant.defaultImageResId?.also { resId ->
                        imagePlant.setImageResource(resId)
                    }
                } else if (!plant.imageUri.isNullOrEmpty()) {
                    Glide.with(imagePlant.context)
                        .load(plant.imageUri)
                        .into(imagePlant)
                } else {
                    imagePlant.setImageResource(R.drawable.plant_1)
                }

                return view
            }
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogPlants)
            .setTitle(getString(R.string.assign_select_plant))
            .setAdapter(adapter) { _, which ->
                val selectedPlant = unassignedPlants[which]
                selectedPlant.room = room // Przypisz roślinę do tego pokoju
                plantStorage.updatePlant(selectedPlant)

                // Odśwież widok po przypisaniu rośliny
                loadRoomData()

                Toast.makeText(this, getString(R.string.plant_has_been_asigned), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
    }

    private fun chooseDefaultImage() {
        // Tworzenie RecyclerView
        val recyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@RoomDetailActivity, 3) // 3 kolumny
            adapter = ImageAdapter(defaultImages) { selectedImageResId ->
                // Po wybraniu zdjęcia ustaw je w ImageView
                imageView.setImageResource(selectedImageResId)
                imageView.tag = selectedImageResId // Zapisz ID zasobu jako tag

                // Aktualizuj pola w obiekcie Room
                room.defaultImageResId = selectedImageResId
                room.isDefaultImage = true
                room.imageUri = null // Resetowanie URI, ponieważ używamy domyślnego zdjęcia

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

    private fun waterAllPlantsInRoom() {
        val datePicker = DatePicker(this)
        val currentDate = Calendar.getInstance()

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

                // Format daty
                val date = "${datePicker.dayOfMonth}-${datePicker.month + 1}-${datePicker.year}"

                // Zaktualizuj daty podlania dla wszystkich roślin w pokoju
                val plants = plantStorage.getPlants().toMutableList()
                plants.forEach { plant ->
                    if (plant.room?.id == room.id) {
                        plant.wateringDates.add(date)
                    }
                }

                // Zapisz zmiany
                plantStorage.savePlants(plants)

                // Odśwież widok
                loadRoomData()

                Toast.makeText(this, getString(R.string.all_plants_watered), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
    }
}