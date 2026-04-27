package com.molfetta.differenziata

import java.time.DayOfWeek

object WasteSchedule {

    data class WasteInfo(
        val emoji: String,
        val title: String,
        val container: String,
        val includes: String?,
        val excludes: String?,
        val colorHex: String,
        val note: String? = null
    )

    fun getWasteForDay(day: DayOfWeek): List<WasteInfo> = when (day) {
        DayOfWeek.MONDAY -> listOf(
            WasteInfo("⚫", "INDIFFERENZIATO", "Mastello / carrellato grigio",
                "Pannolini, pannoloni, assorbenti, stracci, spugne, spazzolini, oggetti di gomma, posate monouso, cicche di sigarette, carta plastificata, carta forno, cocci di ceramica, porcellana e terracotta",
                "Tutto ciò che è separabile/riciclabile, rifiuti urbani pericolosi, ingombranti",
                "#757575")
        )
        DayOfWeek.TUESDAY -> listOf(
            WasteInfo("🟤", "ORGANICO", "Mastello / carrellato marrone",
                "Scarti di cucina, avanzi di cibo e frutta, alimenti avariati, tovaglioli di carta unti, ceneri spente, piccole potature di fiori, piante, sfalci d'erbe, foglie",
                "Pannolini, pannoloni, assorbenti, stracci, spugne, gomme da masticare, cicche di sigarette",
                "#795548"),
            WasteInfo("🟢", "VETRO", "Mastello / carrellato verde", null, null, "#388E3C")
        )
        DayOfWeek.WEDNESDAY -> listOf(
            WasteInfo("🔵", "CARTA, CARTONE e TETRA PAK", "Mastello / carrellato blu",
                "Giornali, riviste, imballaggi di carta e cartoncino, fotocopie e fogli vari, confezioni Tetra Pak (brik latte, vino, succhi di frutta, ecc.)",
                "Carta plastificata, carta forno, ogni tipo di carta/cartone sporcato con vernici o altri prodotti simili",
                "#1565C0",
                "⚠️  Il Tetra Pak va nella CARTA, non nella plastica!")
        )
        DayOfWeek.THURSDAY -> listOf(
            WasteInfo("🟤", "ORGANICO", "Mastello / carrellato marrone",
                "Scarti di cucina, avanzi di cibo e frutta, alimenti avariati, tovaglioli di carta unti, cenere spenta, piccole potature di fiori, piante, sfalci d'erbe, foglie",
                "Pannolini, pannoloni, assorbenti, stracci, spugne, gomme da masticare, cicche di sigarette",
                "#795548")
        )
        DayOfWeek.FRIDAY -> listOf(
            WasteInfo("🟡", "PLASTICA e METALLI", "Sacchi / carrellato gialli",
                "Bottiglie, flaconi per detersivi, piatti e bicchieri monouso, buste, vaschette, pellicole (PLASTICA) — Scatolame, lattine, fogli di alluminio, bombolette spray non T/F, tubetti (METALLI)",
                "Giocattoli, oggetti di gomma, tubi di plastica e metallo, penne",
                "#F9A825")
        )
        DayOfWeek.SATURDAY -> emptyList()
        DayOfWeek.SUNDAY -> listOf(
            WasteInfo("🟤", "ORGANICO", "Mastello / carrellato marrone",
                "Scarti di cucina, avanzi di cibo e frutta, alimenti avariati, tovaglioli di carta unti, cenere spenta, piccole potature di fiori, piante, sfalci d'erbe, foglie",
                "Pannolini, pannoloni, assorbenti, stracci, spugne, gomme da masticare, cicche di sigarette",
                "#795548")
        )
    }
}
