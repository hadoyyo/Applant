//Options.kt
package pl.example.applant

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class Options : Fragment() {

    private lateinit var switchNotifications: Switch
    private lateinit var switchDarkMode: Switch
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationTimeButton: Button
    private lateinit var notificationTimeTextView: TextView
    private lateinit var languageSpinner: Spinner

    private val handler = Handler(Looper.getMainLooper())
    private val checkNotificationPermissionRunnable = object : Runnable {
        override fun run() {
            // Sprawdź stan uprawnień do powiadomień i zaktualizuj przełącznik
            val areNotificationsEnabled = (requireActivity() as MainActivity).areNotificationsEnabled()
            updateSwitchState(areNotificationsEnabled)

            // Uruchom ponownie po 1 sekundzie
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_options, container, false)

        // Inicjalizacja SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // Inicjalizacja przełącznika powiadomień
        switchNotifications = view.findViewById(R.id.switch_notifications)
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        switchNotifications.isChecked = notificationsEnabled

        // Inicjalizacja przycisku i tekstu do wyboru godziny powiadomień
        notificationTimeButton = view.findViewById(R.id.button_set_notification_time)
        notificationTimeTextView = view.findViewById(R.id.text_notification_time)

        // Ustawienie aktualnej godziny powiadomień
        val notificationTime = sharedPreferences.getString("notification_time", "08:00")
        notificationTimeTextView.text = notificationTime

        // Sprawdź, czy powiadomienia są włączone w systemie
        val areNotificationsEnabled = (requireActivity() as MainActivity).areNotificationsEnabled()

        // Ustawienie stanu przełącznika powiadomień
        updateSwitchState(areNotificationsEnabled)

        // Obsługa zmiany stanu przełącznika powiadomień
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val areNotificationsEnabled = (requireActivity() as MainActivity).areNotificationsEnabled()

            if (isChecked && !areNotificationsEnabled) {
                // Jeśli użytkownik próbuje włączyć powiadomienia, ale nie ma uprawnień, pokaż dialog
                showNotificationPermissionDialog()
                switchNotifications.isChecked = false // Przywróć przełącznik do pozycji wyłączonej
            } else {
                // Zapisz nowy stan powiadomień
                sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()

                // Włącz/wyłącz przycisk ustawienia godziny powiadomień
                notificationTimeButton.isEnabled = isChecked

                if (!isChecked) {
                    // Wyłącz powiadomienia
                    PlantStorage(requireContext()).cancelAllNotifications(requireContext())
                }
            }
        }

        // Obsługa kliknięcia przycisku do wyboru godziny powiadomień
        notificationTimeButton.setOnClickListener {
            showTimePickerDialog()
        }

        // Inicjalizacja przełącznika trybu ciemnego
        switchDarkMode = view.findViewById(R.id.switch_dark_mode)

        // Sprawdź, czy tryb ciemny jest włączony systemowo
        val isSystemDarkMode = isSystemDarkModeEnabled(requireContext())
        val darkModeEnabled = sharedPreferences.getBoolean("dark_mode_enabled", isSystemDarkMode)
        switchDarkMode.isChecked = darkModeEnabled

        // Obsługa zmiany stanu przełącznika trybu ciemnego
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            setDarkMode(isChecked)
        }

        // Inicjalizacja Spinnera do wyboru języka
        languageSpinner = view.findViewById(R.id.spinner_language)

        // Lista języków i odpowiadających im flag
        val languages = listOf("English", "Polski")
        val flags = listOf(R.drawable.gb, R.drawable.pl) // Flagi w folderze res/drawable

        // Utwórz i ustaw niestandardowy adapter
        val adapter = CustomSpinnerAdapter(requireContext(), languages, flags)
        languageSpinner.adapter = adapter

        // Ustawienie domyślnego języka w Spinnerze
        val currentLanguage = sharedPreferences.getString("app_language", "en") ?: "en"
        languageSpinner.setSelection(if (currentLanguage == "en") 0 else 1)

        // Obsługa wyboru języka w Spinnerze
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = if (position == 0) "en" else "pl"
                if (currentLanguage != selectedLanguage) {
                    sharedPreferences.edit().putString("app_language", selectedLanguage).apply()
                    setAppLanguage(selectedLanguage)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nie robimy nic
            }
        }

        return view
    }

    // Niestandardowy adapter do wyświetlania flag i tekstu w Spinnerze
    private inner class CustomSpinnerAdapter(
        context: Context,
        private val languages: List<String>,
        private val flags: List<Int>
    ) : ArrayAdapter<String>(context, R.layout.custom_spinner_item, languages) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createView(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createView(position, convertView, parent)
        }

        private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.custom_spinner_item, parent, false)

            val icon = view.findViewById<ImageView>(R.id.icon)
            val text = view.findViewById<TextView>(R.id.text)

            icon.setImageResource(flags[position])
            text.text = languages[position]

            return view
        }
    }

    private fun setAppLanguage(languageCode: String) {
        val resources = requireContext().resources
        val configuration = resources.configuration
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Restart activity, aby zastosować zmiany językowe
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                sharedPreferences.edit().putString("notification_time", time).apply()
                notificationTimeTextView.text = time
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()

        timePickerDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
        timePickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
        timePickerDialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
    }

    private fun updateSwitchState(areNotificationsEnabled: Boolean) {
        if (!areNotificationsEnabled) {
            // Jeśli powiadomienia są wyłączone w systemie, wyłącz przełącznik i ustaw go na "off"
            switchNotifications.isChecked = false
        } else {
            // Jeśli powiadomienia są włączone w systemie, ustaw stan zgodnie z SharedPreferences
            val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
            switchNotifications.isChecked = notificationsEnabled
            switchNotifications.isEnabled = true // Klikalny
        }

        // Ustawienie stanu przycisku ustawienia godziny powiadomień
        notificationTimeButton.isEnabled = switchNotifications.isChecked && areNotificationsEnabled
    }

    private fun showNotificationPermissionDialog() {
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Włącz powiadomienia")
            .setMessage("Aby otrzymywać powiadomienia o podlewaniu roślin, włącz powiadomienia w ustawieniach aplikacji.")
            .setPositiveButton("Przejdź do ustawień") { _, _ ->
                openAppNotificationSettings()
            }
            .setNegativeButton("Później", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
    }

    private fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        }
        startActivity(intent)
    }

    private fun isSystemDarkModeEnabled(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun setDarkMode(enabled: Boolean) {
        val activity = requireActivity()
        val sharedPreferences = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // Zapisz aktywną zakładkę przed restartem
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val lastSelectedTab = bottomNav.selectedItemId

        sharedPreferences.edit()
            .putBoolean("skip_fragment_change", true)
            .putInt("last_selected_tab", lastSelectedTab) // Zapisujemy ostatnią zakładkę
            .apply()

        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Resetuj focus nawigacji, żeby dało się kliknąć "Plants"
        bottomNav.clearFocus()
    }
}