package com.hayatow.nothinggrass

import com.hayatow.nothinggrass.data.CalendarCodec
import com.hayatow.nothinggrass.data.ContributionCalendar
import com.hayatow.nothinggrass.data.ContributionDay
import com.hayatow.nothinggrass.data.GraphQlContributionParser
import com.hayatow.nothinggrass.data.HtmlContributionParser
import com.hayatow.nothinggrass.widget.GrassLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ParsersTest {
    private val html = javaClass.classLoader!!.getResource("contributions_sample.html")!!.readText()

    @Test fun parsesPublicHtmlCalendar() {
        val calendar = HtmlContributionParser.parse(html)
        assertTrue(calendar.days.size in 365..371)
        assertEquals(LocalDate.parse("2025-11-23"), calendar.days.first().date)
        assertTrue(calendar.days.all { it.level in 0..4 })
        assertTrue(calendar.days.any { it.level > 0 })
        assertTrue(calendar.total > 0)
        assertEquals(6, calendar.days.first { it.date == LocalDate.parse("2025-11-30") }.count)
        calendar.days.filter { it.level == 0 }.forEach { assertEquals(0, it.count) }
    }

    @Test fun parsesGraphQlResponse() {
        val json = """{"data":{"user":{"contributionsCollection":{"contributionCalendar":{"weeks":[{"contributionDays":[
            {"date":"2026-01-04","contributionCount":0,"contributionLevel":"NONE"},
            {"date":"2026-01-05","contributionCount":12,"contributionLevel":"FOURTH_QUARTILE"}]}]}}}}}"""
        val calendar = GraphQlContributionParser.parse(json)
        assertEquals(listOf(0, 4), calendar.days.map { it.level })
        assertEquals(12, calendar.total)
        assertTrue(GraphQlContributionParser.requestBody("HayatoW").contains("\"login\":\"HayatoW\""))
    }

    @Test(expected = IllegalStateException::class) fun graphQlErrorsThrow() {
        GraphQlContributionParser.parse("""{"errors":[{"message":"Bad credentials"}]}""")
    }

    @Test fun codecRoundTrips() {
        val calendar = HtmlContributionParser.parse(html)
        assertEquals(calendar, CalendarCodec.decode(CalendarCodec.encode(calendar)))
    }

    @Test fun streakAllowsEmptyToday() {
        fun day(d: Int, c: Int) = ContributionDay(LocalDate.of(2026, 1, d), c, if (c > 0) 1 else 0)
        assertEquals(2, ContributionCalendar(listOf(day(1, 0), day(2, 3), day(3, 1), day(4, 0))).currentStreak)
        assertEquals(3, ContributionCalendar(listOf(day(1, 1), day(2, 3), day(3, 1))).currentStreak)
        assertEquals(0, ContributionCalendar(listOf(day(1, 1), day(2, 0), day(3, 0))).currentStreak)
    }

    @Test fun layoutFitsWidgetSizes() {
        val wide = GrassLayout.compute(360f, 180f, 1f)
        assertTrue(wide.showHeader)
        assertTrue(wide.weeks * wide.cell <= 360f - 32f)
        val slim = GrassLayout.compute(360f, 80f, 1f)
        assertTrue(!slim.showHeader && slim.weeks > wide.weeks)
        assertTrue(GrassLayout.compute(2000f, 400f, 1f).weeks <= 53)
    }
}
