//MainActivity.kt
package pl.example.applant

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import pl.example.applant.databinding.ActivityMainBinding
import java.util.Locale
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var currentTab: Int = R.id.plants // Przechowuje aktualnie wybraną zakładkę

    // GestureDetector do obsługi gestów
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Inicjalizacja GestureDetector
        gestureDetector = GestureDetector(this, SwipeGestureListener())

        // Ustawienie OnTouchListener dla głównego layoutu
        binding.main.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // Ustaw język aplikacji na podstawie SharedPreferences
        val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("app_language", "en") ?: "en"
        val languageChanged = sharedPreferences.getBoolean("language_changed", false)
        if (!languageChanged) {
            setAppLanguage(languageCode)
        } else {
            // Zresetuj flagę po pierwszej zmianie
            sharedPreferences.edit().putBoolean("language_changed", false).apply()
        }

        // Sprawdź, czy tryb ciemny jest włączony w ustawieniach
        val darkModeEnabled = sharedPreferences.getBoolean("dark_mode_enabled", isSystemDarkModeEnabled(this))

        // Ustaw tryb nocny
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            changeStatusBarColor("#18191B")
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            changeStatusBarColor("#f9f9f9")
        }

        // Zawsze ustaw zakładkę "Plants" po uruchomieniu
        val skipFragmentChange = sharedPreferences.getBoolean("skip_fragment_change", false)
        val lastSelectedTab = sharedPreferences.getInt("last_selected_tab", R.id.plants)

        if (!skipFragmentChange) {
            replaceFragment(Plants(), R.id.plants)
            binding.bottomNavigationView.selectedItemId = R.id.plants
        } else {
            // Ustaw ostatnią aktywną zakładkę w navbarze
            binding.bottomNavigationView.selectedItemId = lastSelectedTab
        }

        // Zresetuj flagi po uruchomieniu
        sharedPreferences.edit()
            .putBoolean("skip_fragment_change", false)
            .putInt("last_selected_tab", R.id.plants) // Resetujemy na "Plants"
            .apply()

        // Resetujemy currentTab, żeby poprawnie działała nawigacja
        currentTab = binding.bottomNavigationView.selectedItemId

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obsługa kliknięć w BottomNavigationView
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId != currentTab) {
                when (item.itemId) {
                    R.id.plants -> replaceFragment(Plants(), R.id.plants)
                    R.id.rooms -> replaceFragment(Rooms(), R.id.rooms)
                    R.id.options -> replaceFragment(Options(), R.id.options)
                }
            }
            true
        }

        // Sprawdź, czy powiadomienia są włączone
        if (!areNotificationsEnabled()) {
            showNotificationPermissionDialog()
        }
    }

    private fun changeStatusBarColor(color: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = android.graphics.Color.parseColor(color)
        }
    }

    // Klasa do obsługi gestów przesuwania
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
                        // Przesunięcie w prawo
                        onSwipeRight()
                    } else {
                        // Przesunięcie w lewo
                        onSwipeLeft()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun onSwipeRight() {
        when (currentTab) {
            R.id.rooms -> {
                replaceFragment(Plants(), R.id.plants)
                binding.bottomNavigationView.selectedItemId = R.id.plants
            }
            R.id.options -> {
                replaceFragment(Rooms(), R.id.rooms)
                binding.bottomNavigationView.selectedItemId = R.id.rooms
            }
        }
    }

    private fun onSwipeLeft() {
        when (currentTab) {
            R.id.plants -> {
                replaceFragment(Rooms(), R.id.rooms)
                binding.bottomNavigationView.selectedItemId = R.id.rooms
            }
            R.id.rooms -> {
                replaceFragment(Options(), R.id.options)
                binding.bottomNavigationView.selectedItemId = R.id.options
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, newTab: Int) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        // Wybór animacji w zależności od kierunku przejścia
        when {
            (currentTab == R.id.plants && newTab == R.id.rooms) ||
                    (currentTab == R.id.rooms && newTab == R.id.options) ||
                    (currentTab == R.id.plants && newTab == R.id.options) -> {
                fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            (currentTab == R.id.options && newTab == R.id.rooms) ||
                    (currentTab == R.id.rooms && newTab == R.id.plants) ||
                    (currentTab == R.id.options && newTab == R.id.plants) -> {
                fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }

        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()

        // Zapisz aktualną zakładkę w SharedPreferences
        val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("current_tab", newTab).apply()

        // Aktualizacja obecnie wybranej zakładki
        currentTab = newTab
    }

    private fun isSystemDarkModeEnabled(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    fun areNotificationsEnabled(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }

    private fun showNotificationPermissionDialog() {
        // Tworzenie niestandardowego AlertDialog z zastosowanym stylem
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.enable_notifications))
            .setMessage(R.string.enable_notifications_info)
            .setPositiveButton(R.string.go_to_settings) { _, _ ->
                openAppNotificationSettings()
            }
            .setNegativeButton(R.string.later, null)
            .create()

        // Pokaż okno dialogowe
        dialog.show()

        // Zmiana koloru przycisków
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.purple))
    }

    private fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun setAppLanguage(languageCode: String) {
        val resources = resources
        val configuration = resources.configuration
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Aktualizuj teksty w BottomNavigationView
        updateBottomNavigationViewTexts(languageCode)

        // Ustaw flagę, że język został zmieniony
        val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("language_changed", false).apply()
    }

    private fun updateBottomNavigationViewTexts(languageCode: String) {
        val plantsText = getString(R.string.plants)
        val roomsText = getString(R.string.rooms)
        val optionsText = getString(R.string.options)

        val menu = binding.bottomNavigationView.menu
        menu.findItem(R.id.plants).title = plantsText
        menu.findItem(R.id.rooms).title = roomsText
        menu.findItem(R.id.options).title = optionsText
    }
}