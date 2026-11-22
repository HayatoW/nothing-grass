package com.hayatow.nothinggrass.widget

import android.content.Context
import android.graphics.Color
import android.os.Build
import com.hayatow.nothinggrass.data.ColorMode

/** Colours for the translucent card and dot levels 0..4. */
data class GrassPalette(
    val card: Int,
    val cardHighlight: Int,
    val stroke: Int,
    val text: Int,
    val subText: Int,
    val levels: List<Int>,
    val today: Int,
) {
    companion object {
        const val NOTHING_RED = 0xFFD71921.toInt()
        private val GREEN_DARK = listOf(0x1FFFFFFF, 0xFF0E4429.toInt(), 0xFF006D32.toInt(), 0xFF26A641.toInt(), 0xFF39D353.toInt())
        private val GREEN_LIGHT = listOf(0x1F000000, 0xFF9BE9A8.toInt(), 0xFF40C463.toInt(), 0xFF30A14E.toInt(), 0xFF216E39.toInt())

        fun of(mode: ColorMode, dark: Boolean, accent: Int? = null): GrassPalette {
            val fg = if (dark) Color.WHITE else Color.BLACK
            val levels = when {
                mode == ColorMode.GREEN -> if (dark) GREEN_DARK else GREEN_LIGHT
                mode == ColorMode.WALLPAPER && accent != null -> ramp(accent, dark)
                else -> ramp(fg, dark)
            }
            return GrassPalette(
                card = if (dark) 0x66141414 else 0x8CF5F5F5.toInt(),
                cardHighlight = if (dark) 0x1AFFFFFF else 0x40FFFFFF,
                stroke = if (dark) 0x2EFFFFFF else 0x59FFFFFF,
                text = fg,
                subText = withAlpha(fg, 0.55f),
                levels = levels,
                today = NOTHING_RED,
            )
        }

        private fun ramp(color: Int, dark: Boolean) =
            listOf(if (dark) 0.12f else 0.10f, 0.32f, 0.55f, 0.78f, 1f).map { withAlpha(color, it) }

        fun withAlpha(color: Int, alpha: Float) = (color and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)

        fun forContext(context: Context, mode: ColorMode): GrassPalette {
            val dark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            return of(mode, dark, if (mode == ColorMode.WALLPAPER) wallpaperAccent(context, dark) else null)
        }

        fun wallpaperAccent(context: Context, dark: Boolean): Int? = if (Build.VERSION.SDK_INT >= 31) {
            context.getColor(if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600)
        } else null
    }
}
