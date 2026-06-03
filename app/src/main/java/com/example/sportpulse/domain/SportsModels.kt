package com.example.sportpulse.domain

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class League(
    val id: String,
    val name: String,
    val sport: String,
)

data class MatchInfo(
    val id: String,
    val league: String,
    val sport: String,
    val title: String,
    val homeTeamId: String?,
    val awayTeamId: String?,
    val homeTeam: String,
    val awayTeam: String,
    val date: String,
    val time: String,
    val venue: String,
    val country: String,
    val status: String,
    val round: String,
    val score: String?,
    val imageUrl: String?,
    val leagueBadgeUrl: String?,
    val homeBadgeUrl: String?,
    val awayBadgeUrl: String?,
    val preview: String?,
)

data class TeamInfo(
    val id: String?,
    val name: String,
    val sport: String,
    val league: String,
    val country: String,
    val stadium: String,
    val formedYear: String,
    val badgeUrl: String?,
    val bannerUrl: String?,
    val description: String?,
    val nextMatches: List<MatchInfo> = emptyList(),
    val previousMatches: List<MatchInfo> = emptyList(),
)

data class FavoriteTeam(
    val name: String,
    val sport: String,
    val badgeUrl: String?,
)

data class NewsArticle(
    val title: String,
    val description: String,
    val link: String,
    val publishedAt: String,
    val source: String,
    val imageUrl: String?,
)

val FeaturedLeagues = listOf(
    League(id = "Soccer", name = "Soccer", sport = "Soccer"),
    League(id = "Basketball", name = "Basketball", sport = "Basketball"),
    League(id = "Ice Hockey", name = "Ice Hockey", sport = "Ice Hockey"),
    League(id = "American Football", name = "NFL", sport = "American Football"),
)

fun MatchInfo.shortDateTime(): String {
    val sourceDate = date.takeIf { it.isNotBlank() } ?: return "Date TBD"
    val sourceTime = time.takeIf { it.isNotBlank() } ?: "00:00:00"
    val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    return runCatching {
        formatter.format(parser.parse("$sourceDate $sourceTime")!!)
    }.getOrElse {
        listOf(sourceDate, time.removeSuffix(":00")).filter { it.isNotBlank() }.joinToString(", ")
    }
}
