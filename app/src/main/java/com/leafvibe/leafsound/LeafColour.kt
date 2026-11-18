package com.leafvibe.leafsound

import android.graphics.Color

/** Semantic LeafVibe colors. Every theme keeps the LeafColour visual identity. */
internal object LeafColour {
    data class Palette(
        val name: String,
        val primary: Int,
        val text: Int,
        val accent: Int,
        val background: Int,
        val surface: Int,
        val border: Int,
        val onPrimary: Int
    )

    val themes = linkedMapOf(
        "oasis" to Palette("Oasis · LeafColour original", Color.parseColor("#206B4D"), Color.parseColor("#6B4935"), Color.parseColor("#C5A46D"), Color.parseColor("#DCCBB1"), Color.parseColor("#F0E5D4"), Color.parseColor("#B69A7B"), Color.WHITE),
        "forest" to Palette("Forêt profonde", Color.parseColor("#174A38"), Color.parseColor("#243B32"), Color.parseColor("#A8C686"), Color.parseColor("#D7E1D2"), Color.parseColor("#EEF3E9"), Color.parseColor("#9DAF9A"), Color.WHITE),
        "ocean" to Palette("Lagon", Color.parseColor("#176477"), Color.parseColor("#244B58"), Color.parseColor("#D6B66D"), Color.parseColor("#D5E7E9"), Color.parseColor("#EEF7F7"), Color.parseColor("#9ABFC3"), Color.WHITE),
        "lavender" to Palette("Lavande", Color.parseColor("#65527E"), Color.parseColor("#493D59"), Color.parseColor("#C5A6D8"), Color.parseColor("#E5DDED"), Color.parseColor("#F5F0F8"), Color.parseColor("#B9A9C8"), Color.WHITE),
        "sunset" to Palette("Terracotta", Color.parseColor("#A64F3C"), Color.parseColor("#603D35"), Color.parseColor("#D69A55"), Color.parseColor("#F0DDD0"), Color.parseColor("#FAF0E8"), Color.parseColor("#D0A58F"), Color.WHITE),
        "night" to Palette("Nuit végétale", Color.parseColor("#285C4A"), Color.parseColor("#E5E9E2"), Color.parseColor("#D2B477"), Color.parseColor("#202923"), Color.parseColor("#2B3830"), Color.parseColor("#526456"), Color.WHITE)
    )

    private var active = themes.getValue("oasis")
    fun select(id: String) { active = themes[id] ?: themes.getValue("oasis") }

    val OASIS get() = active.primary
    val WALNUT get() = active.text
    val NATURAL_GOLD get() = active.accent
    val CREAM_DARK get() = active.background
    val SAND_BROWN get() = active.border
    val BACKGROUND get() = active.background
    val PRIMARY get() = active.primary
    val ACCENT get() = active.accent
    val TEXT get() = active.text
    val BORDER get() = active.border
    val SURFACE get() = active.surface
    val ON_PRIMARY get() = active.onPrimary
}
