package com.molfetta.differenziata

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
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

    data class WasteInfo(
        val emoji: String,
        val title: String,
        val container: String,
        val includes: String?,
        val excludes: String?,
        val colorHex: String,
        val note: String? = null
    )

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setSupportActionBar(binding.toolbar)

        // Sposta la toolbar sotto la fotocamera/status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.setPadding(0, top, 0, 0)
            insets
        }

        // Sposta il FAB sopra la barra di navigazione
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabRefresh) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = navBar + (72 * resources.displayMetrics.density).toInt()
            view.layoutParams = params
            insets
        }

        setupDropdown()

        binding.fabRefresh.setOnClickListener {
            setupDropdown()
        }
    }

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
        val shortDateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
        return DayOfWeek.values().map { dow ->
            val date = monday.plusDays(dow.value.toLong() - 1)
            val dayName = dow.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
                .replaceFirstChar { it.uppercase() }
            val marker = if (dow == today.dayOfWeek) " • oggi" else ""
            DayItem(dow, "$dayName  ${date.format(shortDateFmt)}$marker")
        }
    }

    private fun updateUI(dayOfWeek: DayOfWeek) {
        binding.wasteContainer.removeAllViews()

        val wasteItems = getWasteForDay(dayOfWeek)

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

    private fun getWasteForDay(day: DayOfWeek): List<WasteInfo> = when (day) {
        DayOfWeek.MONDAY -> listOf(
            WasteInfo(
                emoji = "⚫",
                title = "INDIFFERENZIATO",
                container = "Mastello / carrellato grigio",
                includes = "Pannolini, pannoloni, assorbenti, stracci, spugne, spazzolini, oggetti di gomma, posate monouso, cicche di sigarette, carta plastificata, carta forno, cocci di ceramica, porcellana e terracotta",
                excludes = "Tutto ciò che è separabile/riciclabile, rifiuti urbani pericolosi, ingombranti",
                colorHex = "#757575"
            )
        )
        DayOfWeek.TUESDAY -> listOf(
            WasteInfo(
                emoji = "🟤",
                title = "ORGANICO",
                container = "Mastello / carrellato marrone",
                includes = "Scarti di cucina, avanzi di cibo e frutta, alimenti avariati, tovaglioli di carta unti, ceneri spente, piccole potature di fiori, piante, sfalci d'erbe, foglie",
                excludes = "Pannolini, pannoloni, assorbenti, stracci, spugne, gomme da masticare, cicche di sigarette",
                colorHex = "#795548"
            ),
            WasteInfo(
                emoji = "🟢",
                title = "VETRO",
                container = "Mastello / carrellato verde",
                includes = null,
                excludes = null,
                colorHex = "#388E3C"
            )
        )
        DayOfWeek.WEDNESDAY -> listOf(
            WasteInfo(
                emoji = "🔵",
                title = "CARTA, CARTONE e TETRA PAK",
                container = "Mastello / carrellato blu",
                includes = "Giornali, riviste, imballaggi di carta e cartoncino, fotocopie e fogli vari, confezioni Tetra Pak (brik latte, vino, succhi di frutta, ecc.)",
                excludes = "Carta plastificata, carta forno, ogni tipo di carta/cartone sporcato con vernici o altri prodotti simili",
                colorHex = "#1565C0",
                note = "⚠️  Il Tetra Pak va nella CARTA, non nella plastica!"
            )
        )
        DayOfWeek.THURSDAY -> listOf(
            WasteInfo(
                emoji = "🟤",
                title = "ORGANICO",
                container = "Mastello / carrellato marrone",
                includes = "Scarti di cucina, avanzi di cibo e frutta, alimenti avariati, tovaglioli di carta unti, cenere spenta, piccole potature di fiori, piante, sfalci d'erbe, foglie",
                excludes = "Pannolini, pannoloni, assorbenti, stracci, spugne, gomme da masticare, cicche di sigarette",
                colorHex = "#795548"
            )
        )
        DayOfWeek.FRIDAY -> listOf(
            WasteInfo(
                emoji = "🟡",
                title = "PLASTICA e METALLI",
                container = "Sacchi / carrellato gialli",
                includes = "Bottiglie, flaconi per detersivi, piatti e bicchieri monouso, buste, vaschette, pellicole (PLASTICA) — Scatolame, lattine, fogli di alluminio, bombolette spray non T/F, tubetti (METALLI)",
                excludes = "Giocattoli, oggetti di gomma, tubi di plastica e metallo, penne",
                colorHex = "#F9A825"
            )
        )
        DayOfWeek.SATURDAY -> emptyList()
        DayOfWeek.SUNDAY -> listOf(
            WasteInfo(
                emoji = "🟤",
                title = "ORGANICO",
                container = "Mastello / carrellato marrone",
                includes = "Scarti di cucina, avanzi di cibo e frutta, alimenti avariati, tovaglioli di carta unti, cenere spenta, piccole potature di fiori, piante, sfalci d'erbe, foglie",
                excludes = "Pannolini, pannoloni, assorbenti, stracci, spugne, gomme da masticare, cicche di sigarette",
                colorHex = "#795548"
            )
        )
    }
}
