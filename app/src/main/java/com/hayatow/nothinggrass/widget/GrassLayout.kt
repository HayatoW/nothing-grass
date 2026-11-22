package com.hayatow.nothinggrass.widget

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Pure layout math: how many weeks fit and how large each dot cell is. */
data class GrassLayout(val showHeader: Boolean, val cell: Float, val weeks: Int, val gridLeft: Float, val gridTop: Float) {
    companion object {
        fun compute(width: Float, height: Float, density: Float): GrassLayout {
            val pad = 16f * density
            val header = 28f * density
            val showHeader = height >= 100f * density
            val gridHeight = height - 2 * pad - if (showHeader) header else 0f
            val gridWidth = width - 2 * pad
            val cell = max(1f, min(gridHeight / 7f, 18f * density))
            val weeks = max(1, min(53, floor(gridWidth / cell).toInt()))
            val left = pad + (gridWidth - weeks * cell) / 2f
            val top = pad + (if (showHeader) header else 0f) + (gridHeight - 7 * cell) / 2f
            return GrassLayout(showHeader, cell, weeks, left, top)
        }
    }
}
