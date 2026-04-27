package com.molfetta.differenziata

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.molfetta.differenziata.databinding.ActivityMainBinding
import com.molfetta.differenziata.databinding.ItemWasteBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : AppCompatActivity() {

    data class DayItem(val dayOfWeek: DayOfWeek, val label: String)

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scheduleAlarm()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.setPadding(0, top, 0, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabRefresh) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = navBar + (72 * resources.displayMetrics.density).toInt()
            view.layoutParams = params
            insets
        }

        setupDropdown()
        setupNotifications()

        binding.fabRefresh.setOnClickListener { setupDropdown() }
    }

    // ── Dropdown ─────────────────────────────────────────────────────────────

    private fun setupDropdown() {
        val dayItems = buildDayItems()
        val labels = dayItems.map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels)
        binding.dropdownDay.setAdapter(adapter)

        val todayIndex = LocalDate.now().dayOfWeek.value - 1
        binding.dropdownDay.setText(labels[todayIndex], false)
        updateUI(dayItems[todayIndex].dayOfWeek)

        binding.dropdownDay.setOnItemClickListener { _, _, position, _ ->
            updateUI(dayItems[position].dayOfWeek)
        }
    }

    private fun buildDayItems(): List<DayItem> {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
        return DayOfWeek.values().map { dow ->
            val date = monday.plusDays(dow.value.toLong() - 1)
            val dayName = dow.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
                .replaceFirstChar { it.uppercase() }
            val marker = if (dow == today.dayOfWeek) " • oggi" else ""
            DayItem(dow, "$dayName  ${date.format(fmt)}$marker")
        }
    }

    private fun updateUI(dayOfWeek: DayOfWeek) {
        binding.wasteContainer.removeAllViews()

        val wasteItems = WasteSchedule.getWasteForDay(dayOfWeek)

        if (wasteItems.isEmpty()) {
            binding.cardNoCollection.visibility = View.VISIBLE
            binding.wasteContainer.visibility = View.GONE
            binding.cardReminder.visibility = View.GONE
        } else {
            binding.cardNoCollection.visibility = View.GONE
            binding.wasteContainer.visibility = View.VISIBLE
            binding.cardReminder.visibility = View.VISIBLE

            val inflater = LayoutInflater.from(this)
            wasteItems.forEach { waste ->
                val item = ItemWasteBinding.inflate(inflater, binding.wasteContainer, false)
                item.colorStrip.setBackgroundColor(Color.parseColor(waste.colorHex))
                item.tvEmoji.text = waste.emoji
                item.tvTitle.text = waste.title
                item.tvContainer.text = waste.container

                if (waste.includes != null) {
                    item.tvIncludes.text = "✅  ${waste.includes}"
                    item.tvIncludes.visibility = View.VISIBLE
                } else {
                    item.tvIncludes.visibility = View.GONE
                }

                if (waste.excludes != null) {
                    item.tvExcludes.text = "❌  ${waste.excludes}"
                    item.tvExcludes.visibility = View.VISIBLE
                } else {
                    item.tvExcludes.visibility = View.GONE
                }

                if (waste.note != null) {
                    item.tvNote.text = waste.note
                    item.tvNote.visibility = View.VISIBLE
                } else {
                    item.tvNote.visibility = View.GONE
                }

                binding.wasteContainer.addView(item.root)
            }
        }
    }

    // ── Notifiche ─────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        // Ripianifica dopo che l'utente torna dalle Impostazioni (permesso potrebbe essere stato concesso)
        if (hasNotificationPermission()) {
            NotificationReceiver.scheduleNextAlarm(this)
        }
    }

    private fun setupNotifications() {
        NotificationReceiver.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED -> scheduleAlarm()
                else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleAlarm()
        }
    }

    private fun scheduleAlarm() {
        // Schedula sempre (esatto se il permesso c'è, inesatto come fallback)
        NotificationReceiver.scheduleNextAlarm(this)

        // Mostra il dialog UNA VOLTA se il permesso exact-alarm manca (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
                if (!prefs.getBoolean("exact_alarm_asked", false)) {
                    prefs.edit().putBoolean("exact_alarm_asked", true).apply()
                    AlertDialog.Builder(this)
                        .setTitle("Promemoria serale")
                        .setMessage("Per ricevere il promemoria esattamente alle 20:30, consenti le sveglie precise nelle impostazioni di sistema.")
                        .setPositiveButton("Apri impostazioni") { _, _ ->
                            startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                            )
                        }
                        .setNegativeButton("Non ora", null)
                        .show()
                }
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }
}
