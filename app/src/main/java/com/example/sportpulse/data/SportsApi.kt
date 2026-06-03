package com.example.sportpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SportsApi(
    private val apiKey: String,
) {
    suspend fun getNextLeagueEvents(sport: String): List<SportsEventDto> = withContext(Dispatchers.IO) {
        nextDates(days = 3)
            .flatMap { date ->
                val url = endpoint("eventsday.php", "d" to date, "s" to sport)
                parseEvents(getJson(url))
            }
            .distinctBy { it.id }
            .sortedWith(compareBy<SportsEventDto> { it.date }.thenBy { it.time })
    }

    suspend fun getEventDetails(eventId: String): SportsEventDto? = withContext(Dispatchers.IO) {
        val url = endpoint("lookupevent.php", "id" to eventId)
        val json = getJson(url)
        parseEvents(json).firstOrNull()
    }

    suspend fun searchTeam(teamName: String): TeamDto? = withContext(Dispatchers.IO) {
        val url = endpoint("searchteams.php", "t" to teamName)
        val json = getJson(url)
        parseTeams(json).firstOrNull()
    }

    suspend fun getNextTeamEvents(teamId: String): List<SportsEventDto> = withContext(Dispatchers.IO) {
        val url = endpoint("eventsnext.php", "id" to teamId)
        parseEvents(getJson(url))
    }

    suspend fun getPreviousTeamEvents(teamId: String): List<SportsEventDto> = withContext(Dispatchers.IO) {
        val url = endpoint("eventslast.php", "id" to teamId)
        parseEvents(getJson(url))
    }

    private fun endpoint(path: String, vararg params: Pair<String, String>): String {
        val query = params.joinToString("&") { (key, value) ->
            "${key}=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "$BASE_URL/$apiKey/$path?$query"
    }

    private fun getJson(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
        }

        return try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) {
                error("API returned HTTP ${connection.responseCode}: $body")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun parseEvents(json: String): List<SportsEventDto> {
        val root = JSONObject(json)
        val events = root.optJSONArray("events") ?: return emptyList()

        return buildList {
            for (index in 0 until events.length()) {
                val event = events.optJSONObject(index) ?: continue
                add(
                    SportsEventDto(
                        id = event.optStringOrNull("idEvent"),
                        league = event.optStringOrNull("strLeague"),
                        season = event.optStringOrNull("strSeason"),
                        sport = event.optStringOrNull("strSport"),
                        homeTeamId = event.optStringOrNull("idHomeTeam"),
                        awayTeamId = event.optStringOrNull("idAwayTeam"),
                        homeTeam = event.optStringOrNull("strHomeTeam"),
                        awayTeam = event.optStringOrNull("strAwayTeam"),
                        date = event.optStringOrNull("dateEvent"),
                        time = event.optStringOrNull("strTime"),
                        venue = event.optStringOrNull("strVenue"),
                        country = event.optStringOrNull("strCountry"),
                        status = event.optStringOrNull("strStatus"),
                        round = event.optStringOrNull("intRound"),
                        homeScore = event.optStringOrNull("intHomeScore"),
                        awayScore = event.optStringOrNull("intAwayScore"),
                        thumbnail = event.optStringOrNull("strThumb"),
                        leagueBadge = event.optStringOrNull("strLeagueBadge"),
                        homeTeamBadge = event.optStringOrNull("strHomeTeamBadge"),
                        awayTeamBadge = event.optStringOrNull("strAwayTeamBadge"),
                        preview = event.optStringOrNull("strPreview"),
                    ),
                )
            }
        }
    }

    private fun parseTeams(json: String): List<TeamDto> {
        val root = JSONObject(json)
        val teams = root.optJSONArray("teams") ?: return emptyList()

        return buildList {
            for (index in 0 until teams.length()) {
                val team = teams.optJSONObject(index) ?: continue
                add(
                    TeamDto(
                        id = team.optStringOrNull("idTeam"),
                        name = team.optStringOrNull("strTeam"),
                        sport = team.optStringOrNull("strSport"),
                        league = team.optStringOrNull("strLeague"),
                        country = team.optStringOrNull("strCountry"),
                        stadium = team.optStringOrNull("strStadium"),
                        formedYear = team.optStringOrNull("intFormedYear"),
                        badge = team.optStringOrNull("strBadge"),
                        banner = team.optStringOrNull("strBanner"),
                        description = team.optStringOrNull("strDescriptionEN"),
                    ),
                )
            }
        }
    }

    private fun JSONObject.optStringOrNull(name: String): String? {
        val value = optString(name, "").trim()
        return value.takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun nextDates(days: Int): List<String> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        return List(days) {
            formatter.format(calendar.time).also {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    private companion object {
        const val BASE_URL = "https://www.thesportsdb.com/api/v1/json"
        const val TIMEOUT_MILLIS = 12_000
    }
}

data class SportsEventDto(
    val id: String?,
    val league: String?,
    val season: String?,
    val sport: String?,
    val homeTeamId: String?,
    val awayTeamId: String?,
    val homeTeam: String?,
    val awayTeam: String?,
    val date: String?,
    val time: String?,
    val venue: String?,
    val country: String?,
    val status: String?,
    val round: String?,
    val homeScore: String?,
    val awayScore: String?,
    val thumbnail: String?,
    val leagueBadge: String?,
    val homeTeamBadge: String?,
    val awayTeamBadge: String?,
    val preview: String?,
)

data class TeamDto(
    val id: String?,
    val name: String?,
    val sport: String?,
    val league: String?,
    val country: String?,
    val stadium: String?,
    val formedYear: String?,
    val badge: String?,
    val banner: String?,
    val description: String?,
)
