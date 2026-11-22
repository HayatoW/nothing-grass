package com.hayatow.nothinggrass.data

import java.time.LocalDate

data class ContributionDay(val date: LocalDate, val count: Int, val level: Int)

data class ContributionCalendar(val days: List<ContributionDay>) {
    val total: Int get() = days.sumOf { it.count }

    /** Consecutive days with contributions, ending today or yesterday (today may still be empty). */
    val currentStreak: Int
        get() {
            val sorted = days.sortedBy { it.date }
            var i = sorted.lastIndex
            if (i >= 0 && sorted[i].count == 0) i--
            var streak = 0
            while (i >= 0 && sorted[i].count > 0) { streak++; i-- }
            return streak
        }
}
