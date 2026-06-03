package com.newswinnerapp.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.newswinnerapp.app.data.FavoritesStore
import com.newswinnerapp.app.data.NewsRepository
import com.newswinnerapp.app.data.SportsRepository
import com.newswinnerapp.app.domain.FavoriteTeam
import com.newswinnerapp.app.domain.FeaturedLeagues
import com.newswinnerapp.app.domain.League
import com.newswinnerapp.app.domain.MatchInfo
import com.newswinnerapp.app.domain.NewsArticle
import com.newswinnerapp.app.domain.TeamInfo
import com.newswinnerapp.app.notifications.MatchNotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SportsViewModel(
    private val sportsRepository: SportsRepository,
    private val newsRepository: NewsRepository,
    private val favoritesStore: FavoritesStore,
    private val notificationScheduler: MatchNotificationScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SportsUiState(favorites = favoritesStore.load()),
    )
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    init {
        notificationScheduler.ensureChannel()
        refresh()
        refreshNews()
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == AppTab.News && _uiState.value.news.isEmpty()) {
            refreshNews()
        }
    }

    fun selectLeague(league: League) {
        if (league.id == _uiState.value.selectedLeague.id) return
        _uiState.update {
            it.copy(
                selectedLeague = league,
                selectedMatch = null,
                selectedTeam = null,
                detailLoading = false,
                news = emptyList(),
            )
        }
        refresh()
        refreshNews()
    }

    fun refresh() {
        val league = _uiState.value.selectedLeague
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { sportsRepository.nextMatches(league.id) }
                .onSuccess { matches ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            matches = matches,
                            selectedMatch = matches.firstOrNull(),
                            error = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            matches = emptyList(),
                            selectedMatch = null,
                            error = throwable.message ?: "Could not load matches",
                        )
                    }
                }
        }
    }

    fun refreshNews() {
        val sport = _uiState.value.selectedLeague.id
        viewModelScope.launch {
            _uiState.update { it.copy(newsLoading = true, newsError = null) }
            runCatching { newsRepository.latestNews(sport) }
                .onSuccess { news ->
                    _uiState.update { it.copy(newsLoading = false, news = news, newsError = null) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            newsLoading = false,
                            news = emptyList(),
                            newsError = throwable.message ?: "Could not load news",
                        )
                    }
                }
        }
    }

    fun selectMatch(match: MatchInfo) {
        _uiState.update {
            it.copy(selectedMatch = match, selectedTeam = null, detailLoading = true)
        }
        viewModelScope.launch {
            runCatching { sportsRepository.matchDetails(match.id) }
                .onSuccess { details ->
                    _uiState.update {
                        it.copy(
                            selectedMatch = details ?: match,
                            detailLoading = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state -> state.copy(detailLoading = false) }
                }
        }
    }

    fun openTeam(teamName: String) {
        _uiState.update { it.copy(teamLoading = true, teamError = null, selectedTab = AppTab.Team) }
        viewModelScope.launch {
            runCatching { sportsRepository.teamDetails(teamName) }
                .onSuccess { team ->
                    _uiState.update {
                        it.copy(
                            selectedTeam = team,
                            teamLoading = false,
                            teamError = if (team == null) "Team not found" else null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            selectedTeam = null,
                            teamLoading = false,
                            teamError = throwable.message ?: "Could not load team",
                        )
                    }
                }
        }
    }

    fun toggleFavorite(team: FavoriteTeam) {
        val updated = _uiState.value.favorites.toMutableList()
        val index = updated.indexOfFirst { it.name == team.name }
        if (index >= 0) {
            updated.removeAt(index)
        } else {
            updated += team
        }
        favoritesStore.save(updated)
        _uiState.update { it.copy(favorites = updated) }
    }

    fun scheduleReminder(match: MatchInfo, minutesBefore: Int) {
        val scheduled = notificationScheduler.schedule(match, minutesBefore)
        _uiState.update {
            it.copy(
                snackbarMessage = if (scheduled) {
                    if (minutesBefore == 0) "Start-time reminder added" else "$minutesBefore min. reminder added"
                } else {
                    "Could not schedule reminder: no future match time"
                },
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

enum class AppTab(val label: String) {
    Matches("Matches"),
    News("News"),
    Favorites("Favorites"),
    Team("Team"),
}

data class SportsUiState(
    val leagues: List<League> = FeaturedLeagues,
    val selectedLeague: League = FeaturedLeagues.first(),
    val selectedTab: AppTab = AppTab.Matches,
    val matches: List<MatchInfo> = emptyList(),
    val selectedMatch: MatchInfo? = null,
    val isLoading: Boolean = false,
    val detailLoading: Boolean = false,
    val error: String? = null,
    val news: List<NewsArticle> = emptyList(),
    val newsLoading: Boolean = false,
    val newsError: String? = null,
    val favorites: List<FavoriteTeam> = emptyList(),
    val selectedTeam: TeamInfo? = null,
    val teamLoading: Boolean = false,
    val teamError: String? = null,
    val snackbarMessage: String? = null,
)

class SportsViewModelFactory(
    private val sportsRepository: SportsRepository,
    private val newsRepository: NewsRepository,
    private val favoritesStore: FavoritesStore,
    private val notificationScheduler: MatchNotificationScheduler,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SportsViewModel(
            sportsRepository = sportsRepository,
            newsRepository = newsRepository,
            favoritesStore = favoritesStore,
            notificationScheduler = notificationScheduler,
        ) as T
    }
}
