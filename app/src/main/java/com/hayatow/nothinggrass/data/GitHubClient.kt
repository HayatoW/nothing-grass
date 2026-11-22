package com.hayatow.nothinggrass.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object GitHubClient {
    private const val USER_AGENT = "nothing-grass-android"

    fun fetch(login: String, token: String?): ContributionCalendar =
        if (token.isNullOrBlank()) fetchHtml(login) else fetchGraphQl(login, token)

    private fun fetchHtml(login: String): ContributionCalendar {
        val url = URL("https://github.com/users/${URLEncoder.encode(login, "UTF-8")}/contributions")
        return HtmlContributionParser.parse(request(url, "GET", null, emptyMap()))
    }

    private fun fetchGraphQl(login: String, token: String): ContributionCalendar {
        val body = GraphQlContributionParser.requestBody(login)
        val json = request(URL("https://api.github.com/graphql"), "POST", body, mapOf("Authorization" to "Bearer $token"))
        return GraphQlContributionParser.parse(json)
    }

    private fun request(url: URL, method: String, body: String?, headers: Map<String, String>): String {
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = conn.responseCode
            if (code == 404) error("ユーザーが見つかりません")
            if (code == 401) error("トークンが無効です")
            if (code !in 200..299) error("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
