package com.hayatow.nothinggrass.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Parses `https://github.com/users/<login>/contributions` (public HTML, no token). */
object HtmlContributionParser {
    private val dayCell = Regex("""<td\b[^>]*ContributionCalendar-day[^>]*>""")
    private val tooltip = Regex("""<tool-tip\b[^>]*\bfor="([^"]+)"[^>]*>([^<]*)""")
    private val countText = Regex("""^\s*([\d,]+) contributions?""")

    private fun attr(tag: String, name: String) = Regex("""\b$name="([^"]*)"""").find(tag)?.groupValues?.get(1)

    fun parse(html: String): ContributionCalendar {
        val counts = tooltip.findAll(html).associate { m ->
            m.groupValues[1] to (countText.find(m.groupValues[2])?.groupValues?.get(1)?.replace(",", "")?.toInt() ?: 0)
        }
        val days = dayCell.findAll(html).mapNotNull { m ->
            val tag = m.value
            val date = attr(tag, "data-date") ?: return@mapNotNull null
            val level = attr(tag, "data-level")?.toIntOrNull() ?: 0
            val count = attr(tag, "id")?.let { counts[it] } ?: if (level > 0) 1 else 0
            ContributionDay(LocalDate.parse(date), count, level)
        }.sortedBy { it.date }.toList()
        require(days.isNotEmpty()) { "No contribution cells found" }
        return ContributionCalendar(days)
    }
}

/** Builds and parses the GitHub GraphQL `contributionCalendar` query (needs a token). */
object GraphQlContributionParser {
    private const val QUERY = "query(\$login:String!){user(login:\$login){contributionsCollection{contributionCalendar{" +
        "weeks{contributionDays{date contributionCount contributionLevel}}}}}}"

    fun requestBody(login: String): String =
        JSONObject().put("query", QUERY).put("variables", JSONObject().put("login", login)).toString()

    fun parse(json: String): ContributionCalendar {
        val root = JSONObject(json)
        root.optJSONArray("errors")?.let { if (it.length() > 0) error(it.getJSONObject(0).optString("message")) }
        val user = root.optJSONObject("data")?.optJSONObject("user") ?: error("User not found")
        val weeks = user.getJSONObject("contributionsCollection").getJSONObject("contributionCalendar").getJSONArray("weeks")
        val days = mutableListOf<ContributionDay>()
        for (w in 0 until weeks.length()) {
            val list = weeks.getJSONObject(w).getJSONArray("contributionDays")
            for (d in 0 until list.length()) {
                val day = list.getJSONObject(d)
                days += ContributionDay(
                    LocalDate.parse(day.getString("date")),
                    day.getInt("contributionCount"),
                    levelOf(day.getString("contributionLevel")),
                )
            }
        }
        return ContributionCalendar(days.sortedBy { it.date })
    }

    fun levelOf(name: String) = when (name) {
        "FIRST_QUARTILE" -> 1
        "SECOND_QUARTILE" -> 2
        "THIRD_QUARTILE" -> 3
        "FOURTH_QUARTILE" -> 4
        else -> 0
    }
}

/** Compact JSON form used to cache the last fetched calendar. */
object CalendarCodec {
    fun encode(calendar: ContributionCalendar): String = JSONArray().apply {
        calendar.days.forEach { put(JSONArray().put(it.date.toString()).put(it.count).put(it.level)) }
    }.toString()

    fun decode(json: String): ContributionCalendar {
        val array = JSONArray(json)
        return ContributionCalendar((0 until array.length()).map {
            val d = array.getJSONArray(it)
            ContributionDay(LocalDate.parse(d.getString(0)), d.getInt(1), d.getInt(2))
        })
    }
}
