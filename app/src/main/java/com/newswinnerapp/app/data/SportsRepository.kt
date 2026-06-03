package com.newswinnerapp.app.data

import com.newswinnerapp.app.domain.MatchInfo
import com.newswinnerapp.app.domain.TeamInfo

class SportsRepository(
    private val api: SportsApi,
) {
    suspend fun nextMatches(leagueId: String): List<MatchInfo> =
        api.getNextLeagueEvents(leagueId).mapNotNull { it.toDomain() }

    suspend fun matchDetails(eventId: String): MatchInfo? =
        api.getEventDetails(eventId)?.toDomain()

    suspend fun teamDetails(teamName: String): TeamInfo? {
        val team = api.searchTeam(teamName) ?: return null
        val teamId = team.id
        val nextMatches = teamId?.let { api.getNextTeamEvents(it).mapNotNull { event -> event.toDomain() } }.orEmpty()
        val previousMatches = teamId?.let { api.getPreviousTeamEvents(it).mapNotNull { event -> event.toDomain() } }.orEmpty()

        return TeamInfo(
            id = team.id,
            name = team.name ?: teamName,
            sport = team.sport.orEmpty(),
            league = team.league.orEmpty(),
            country = team.country.orEmpty(),
            stadium = team.stadium.orEmpty(),
            formedYear = team.formedYear.orEmpty(),
            badgeUrl = team.badge,
            bannerUrl = team.banner,
            description = team.description,
            nextMatches = nextMatches,
            previousMatches = previousMatches,
        )
    }

    private fun SportsEventDto.toDomain(): MatchInfo? {
        val safeId = id ?: return null
        val safeHome = homeTeam ?: "Home"
        val safeAway = awayTeam ?: "Away"
        val score = when {
            !homeScore.isNullOrBlank() && !awayScore.isNullOrBlank() -> "$homeScore : $awayScore"
            else -> null
        }

        return MatchInfo(
            id = safeId,
            league = league.orEmpty(),
            sport = sport.orEmpty(),
            title = "$safeHome vs $safeAway",
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId,
            homeTeam = safeHome,
            awayTeam = safeAway,
            date = date.orEmpty(),
            time = time.orEmpty(),
            venue = venue.orEmpty(),
            country = country.orEmpty(),
            status = status ?: "Scheduled",
            round = round.orEmpty(),
            score = score,
            imageUrl = thumbnail ?: leagueBadge ?: homeTeamBadge ?: awayTeamBadge,
            leagueBadgeUrl = leagueBadge,
            homeBadgeUrl = homeTeamBadge,
            awayBadgeUrl = awayTeamBadge,
            preview = preview,
        )
    }
}
