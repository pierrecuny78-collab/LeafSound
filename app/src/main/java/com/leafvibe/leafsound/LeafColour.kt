package com.leafvibe.leafsound

import android.graphics.Color

/** Shared LeafVibe palette. Keep semantic tokens consistent across Leaf apps. */
internal object LeafColour {
    val OASIS = Color.parseColor("#206B4D")
    val WALNUT = Color.parseColor("#6B4935")
    val NATURAL_GOLD = Color.parseColor("#C5A46D")
    val CREAM_DARK = Color.parseColor("#DCCBB1")
    val SAND_BROWN = Color.parseColor("#B69A7B")

    val BACKGROUND get() = CREAM_DARK
    val PRIMARY get() = OASIS
    val ACCENT get() = NATURAL_GOLD
    val TEXT get() = WALNUT
    val BORDER get() = SAND_BROWN
}
