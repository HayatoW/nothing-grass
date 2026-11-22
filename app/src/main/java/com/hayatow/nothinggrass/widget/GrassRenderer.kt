package com.hayatow.nothinggrass.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.hayatow.nothinggrass.R
import com.hayatow.nothinggrass.data.ContributionCalendar
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Draws the whole widget (translucent card, header, dot grid) into a bitmap. */
class GrassRenderer(context: Context) {
    private val density = context.resources.displayMetrics.density
    private val sans: Typeface = ResourcesCompat.getFont(context, R.font.geist_medium) ?: Typeface.DEFAULT
    private val mono: Typeface = ResourcesCompat.getFont(context, R.font.geist_mono_medium) ?: Typeface.MONOSPACE

    fun render(
        widthPx: Int,
        heightPx: Int,
        login: String,
        calendar: ContributionCalendar?,
        palette: GrassPalette,
        today: LocalDate = LocalDate.now(),
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val radius = 24f * density
        val stroke = 1f * density
        val card = RectF(stroke / 2, stroke / 2, w - stroke / 2, h - stroke / 2)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = palette.card
        canvas.drawRoundRect(card, radius, radius, paint)
        paint.shader = LinearGradient(0f, 0f, w * 0.6f, h, palette.cardHighlight, 0, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(card, radius, radius, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.color = palette.stroke
        canvas.drawRoundRect(card, radius, radius, paint)
        paint.style = Paint.Style.FILL

        val layout = GrassLayout.compute(w, h, density)
        val pad = 16f * density
        if (layout.showHeader) {
            val baseline = pad + 16f * density
            val left = pad + 2 * density
            val right = w - pad - 2 * density
            val gap = 8f * density
            paint.typeface = mono
            paint.textSize = 12f * density
            val total = calendar?.let { NumberFormat.getIntegerInstance().format(it.total) } ?: "--"
            val streak = calendar?.currentStreak ?: 0
            val stats = listOf("$total contrib · ${streak}d", "$total · ${streak}d", total)
                .firstOrNull { paint.measureText(it) <= (right - left) * 0.6f }
            val statsWidth = stats?.let { paint.measureText(it) + gap } ?: 0f
            if (stats != null) {
                paint.color = palette.subText
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(stats, right, baseline, paint)
            }
            paint.typeface = sans
            paint.textSize = 14f * density
            paint.color = palette.text
            paint.textAlign = Paint.Align.LEFT
            val name = TextUtils.ellipsize(login.ifBlank { "GitHub" }, TextPaint(paint), right - left - statsWidth, TextUtils.TruncateAt.END)
            canvas.drawText(name.toString(), left, baseline, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        val byDate = calendar?.days?.associateBy { it.date }.orEmpty()
        val lastDay = calendar?.days?.maxOfOrNull { it.date }?.takeIf { it.isAfter(today) } ?: today
        val lastSunday = lastDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val firstSunday = lastSunday.minusWeeks((layout.weeks - 1).toLong())
        val dot = layout.cell * 0.36f
        for (week in 0 until layout.weeks) {
            for (dow in 0 until 7) {
                val date = firstSunday.plusDays(week * 7L + dow)
                if (date.isAfter(lastDay)) continue
                val cx = layout.gridLeft + week * layout.cell + layout.cell / 2
                val cy = layout.gridTop + dow * layout.cell + layout.cell / 2
                val day = byDate[date]
                val level = (day?.level ?: 0).coerceIn(0, 4)
                paint.color = palette.levels[level]
                canvas.drawCircle(cx, cy, dot, paint)
                if (date == lastDay) {
                    paint.color = palette.today
                    if ((day?.count ?: 0) > 0) {
                        canvas.drawCircle(cx, cy, dot, paint)
                    } else {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1.5f * density
                        canvas.drawCircle(cx, cy, dot, paint)
                        paint.style = Paint.Style.FILL
                    }
                }
            }
        }
        return bitmap
    }
}
