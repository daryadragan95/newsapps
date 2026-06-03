package com.example.sportpulse.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.sportpulse.domain.FavoriteTeam
import com.example.sportpulse.domain.League
import com.example.sportpulse.domain.MatchInfo
import com.example.sportpulse.domain.NewsArticle
import com.example.sportpulse.domain.TeamInfo
import com.example.sportpulse.domain.shortDateTime

@Composable
fun SportPulseApp(viewModel: SportsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = uiState.snackbarMessage

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            Header(
                selectedLeague = uiState.selectedLeague,
                onRefresh = {
                    when (uiState.selectedTab) {
                        AppTab.News -> viewModel.refreshNews()
                        else -> viewModel.refresh()
                    }
                },
                isLoading = uiState.isLoading || uiState.newsLoading || uiState.teamLoading,
            )
        },
        bottomBar = {
            BottomNavigation(
                selectedTab = uiState.selectedTab,
                showTeamTab = uiState.selectedTeam != null || uiState.teamLoading || uiState.selectedTab == AppTab.Team,
                onTabClick = viewModel::selectTab,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (uiState.selectedTab) {
            AppTab.Matches -> MatchesContent(
                uiState = uiState,
                padding = padding,
                onLeagueClick = viewModel::selectLeague,
                onRetry = viewModel::refresh,
                onMatchClick = viewModel::selectMatch,
                onOpenTeam = viewModel::openTeam,
                onToggleFavorite = viewModel::toggleFavorite,
                onScheduleReminder = viewModel::scheduleReminder,
            )

            AppTab.News -> NewsContent(
                uiState = uiState,
                padding = padding,
                onLeagueClick = viewModel::selectLeague,
                onRetry = viewModel::refreshNews,
            )

            AppTab.Favorites -> FavoritesContent(
                uiState = uiState,
                padding = padding,
                onOpenTeam = viewModel::openTeam,
                onRemoveFavorite = viewModel::toggleFavorite,
                onMatchClick = viewModel::selectMatch,
            )

            AppTab.Team -> TeamContent(
                uiState = uiState,
                padding = padding,
                onOpenTeam = viewModel::openTeam,
                onToggleFavorite = viewModel::toggleFavorite,
                onMatchClick = viewModel::selectMatch,
            )
        }
    }
}

@Composable
private fun Header(
    selectedLeague: League,
    onRefresh: () -> Unit,
    isLoading: Boolean,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SportPulse",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Matches, news, and teams: ${selectedLeague.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onRefresh,
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(if (isLoading) "..." else "Refresh")
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    selectedTab: AppTab,
    showTeamTab: Boolean,
    onTabClick: (AppTab) -> Unit,
) {
    val tabs = buildList {
        add(AppTab.Matches)
        add(AppTab.News)
        add(AppTab.Favorites)
        if (showTeamTab) add(AppTab.Team)
    }
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabClick(tab) },
                icon = { Text(tab.label.take(1)) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun MatchesContent(
    uiState: SportsUiState,
    padding: PaddingValues,
    onLeagueClick: (League) -> Unit,
    onRetry: () -> Unit,
    onMatchClick: (MatchInfo) -> Unit,
    onOpenTeam: (String) -> Unit,
    onToggleFavorite: (FavoriteTeam) -> Unit,
    onScheduleReminder: (MatchInfo, Int) -> Unit,
) {
    val selectedMatch = uiState.selectedMatch
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LeagueSelector(
                leagues = uiState.leagues,
                selectedLeague = uiState.selectedLeague,
                onLeagueClick = onLeagueClick,
            )
        }

        uiState.error?.let { error ->
            item { ErrorPanel(message = error, onRetry = onRetry) }
        }

        if (selectedMatch != null) {
            item {
                MatchDetails(
                    match = selectedMatch,
                    isLoading = uiState.detailLoading,
                    favorites = uiState.favorites,
                    onOpenTeam = onOpenTeam,
                    onToggleFavorite = onToggleFavorite,
                    onScheduleReminder = onScheduleReminder,
                )
            }
        }

        item {
            SectionTitle(
                title = "Upcoming Matches",
                subtitle = "Next 3 days: ${uiState.selectedLeague.name}",
            )
        }

        when {
            uiState.isLoading && uiState.matches.isEmpty() -> item { LoadingPanel() }
            uiState.matches.isEmpty() && uiState.error == null -> item { EmptyPanel() }
            else -> items(uiState.matches, key = { it.id }) { match ->
                MatchCard(
                    match = match,
                    selected = match.id == selectedMatch?.id,
                    onClick = { onMatchClick(match) },
                )
            }
        }
    }
}

@Composable
private fun NewsContent(
    uiState: SportsUiState,
    padding: PaddingValues,
    onLeagueClick: (League) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LeagueSelector(
                leagues = uiState.leagues,
                selectedLeague = uiState.selectedLeague,
                onLeagueClick = onLeagueClick,
            )
        }

        item {
            SectionTitle(
                title = "News",
                subtitle = "ESPN RSS feed for ${uiState.selectedLeague.name}",
            )
        }

        uiState.newsError?.let { error ->
            item { ErrorPanel(message = error, onRetry = onRetry) }
        }

        when {
            uiState.newsLoading && uiState.news.isEmpty() -> item { LoadingPanel() }
            uiState.news.isEmpty() && uiState.newsError == null -> item {
                EmptyText("No news for this section yet.")
            }

            else -> items(uiState.news, key = { it.link }) { article ->
                NewsCard(article = article)
            }
        }
    }
}

@Composable
private fun FavoritesContent(
    uiState: SportsUiState,
    padding: PaddingValues,
    onOpenTeam: (String) -> Unit,
    onRemoveFavorite: (FavoriteTeam) -> Unit,
    onMatchClick: (MatchInfo) -> Unit,
) {
    val favoriteNames = uiState.favorites.map { it.name }.toSet()
    val favoriteMatches = uiState.matches.filter {
        it.homeTeam in favoriteNames || it.awayTeam in favoriteNames
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle(title = "Favorite Teams", subtitle = "Saved locally on this device") }

        if (uiState.favorites.isEmpty()) {
            item { EmptyText("Add teams from a match card or team screen.") }
        } else {
            items(uiState.favorites, key = { it.name }) { team ->
                FavoriteTeamCard(
                    team = team,
                    onOpen = { onOpenTeam(team.name) },
                    onRemove = { onRemoveFavorite(team) },
                )
            }
        }

        item { SectionTitle(title = "Favorite Matches", subtitle = uiState.selectedLeague.name) }

        if (favoriteMatches.isEmpty()) {
            item { EmptyText("No matches for favorite teams in the current feed.") }
        } else {
            items(favoriteMatches, key = { it.id }) { match ->
                MatchCard(
                    match = match,
                    selected = match.id == uiState.selectedMatch?.id,
                    onClick = { onMatchClick(match) },
                )
            }
        }
    }
}

@Composable
private fun TeamContent(
    uiState: SportsUiState,
    padding: PaddingValues,
    onOpenTeam: (String) -> Unit,
    onToggleFavorite: (FavoriteTeam) -> Unit,
    onMatchClick: (MatchInfo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (uiState.teamLoading) {
            item { LoadingPanel() }
        }

        uiState.teamError?.let { error ->
            item {
                ErrorPanel(
                    message = error,
                    onRetry = { uiState.selectedMatch?.homeTeam?.let(onOpenTeam) },
                )
            }
        }

        val team = uiState.selectedTeam
        if (team == null && !uiState.teamLoading) {
            item { EmptyText("Open a team from a match card or favorites.") }
        } else if (team != null) {
            item {
                TeamProfile(
                    team = team,
                    isFavorite = uiState.favorites.any { it.name == team.name },
                    onToggleFavorite = {
                        onToggleFavorite(
                            FavoriteTeam(
                                name = team.name,
                                sport = team.sport,
                                badgeUrl = team.badgeUrl,
                            ),
                        )
                    },
                )
            }

            item { SectionTitle(title = "Upcoming Matches", subtitle = team.name) }
            if (team.nextMatches.isEmpty()) {
                item { EmptyText("The API did not return upcoming matches for this team.") }
            } else {
                items(team.nextMatches, key = { it.id }) { match ->
                    MatchCard(match = match, selected = false, onClick = { onMatchClick(match) })
                }
            }

            item { SectionTitle(title = "Recent Matches", subtitle = team.name) }
            if (team.previousMatches.isEmpty()) {
                item { EmptyText("The API did not return recent matches for this team.") }
            } else {
                items(team.previousMatches, key = { it.id }) { match ->
                    MatchCard(match = match, selected = false, onClick = { onMatchClick(match) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeagueSelector(
    leagues: List<League>,
    selectedLeague: League,
    onLeagueClick: (League) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Sports",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            leagues.forEach { league ->
                FilterChip(
                    selected = league.id == selectedLeague.id,
                    onClick = { onLeagueClick(league) },
                    label = { Text(league.name) },
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchDetails(
    match: MatchInfo,
    isLoading: Boolean,
    favorites: List<FavoriteTeam>,
    onOpenTeam: (String) -> Unit,
    onToggleFavorite: (FavoriteTeam) -> Unit,
    onScheduleReminder: (MatchInfo, Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MatchHeroImage(match = match)

            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot()
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isLoading) "Updating details" else "Match Card",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Text(
                text = match.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            TeamActions(
                match = match,
                favorites = favorites,
                onOpenTeam = onOpenTeam,
                onToggleFavorite = onToggleFavorite,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatPill(label = "Date", value = match.shortDateTime())
                StatPill(label = "Status", value = match.status)
                StatPill(label = "League", value = match.league.ifBlank { "Not specified" })
                StatPill(label = "Round", value = match.round.ifBlank { "N/A" })
            }

            ReminderActions(match = match, onScheduleReminder = onScheduleReminder)

            Text(
                text = match.preview
                    ?: "Detailed stats depend on API coverage. Schedule, league, date, time, venue, status, and score are available when published.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun TeamActions(
    match: MatchInfo,
    favorites: List<FavoriteTeam>,
    onOpenTeam: (String) -> Unit,
    onToggleFavorite: (FavoriteTeam) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TeamActionRow(
            name = match.homeTeam,
            badgeUrl = match.homeBadgeUrl ?: match.leagueBadgeUrl,
            isFavorite = favorites.any { it.name == match.homeTeam },
            onOpen = { onOpenTeam(match.homeTeam) },
            onToggleFavorite = {
                onToggleFavorite(FavoriteTeam(match.homeTeam, match.sport, match.homeBadgeUrl ?: match.leagueBadgeUrl))
            },
        )
        TeamActionRow(
            name = match.awayTeam,
            badgeUrl = match.awayBadgeUrl ?: match.leagueBadgeUrl,
            isFavorite = favorites.any { it.name == match.awayTeam },
            onOpen = { onOpenTeam(match.awayTeam) },
            onToggleFavorite = {
                onToggleFavorite(FavoriteTeam(match.awayTeam, match.sport, match.awayBadgeUrl ?: match.leagueBadgeUrl))
            },
        )
    }
}

@Composable
private fun TeamActionRow(
    name: String,
    badgeUrl: String?,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BadgeImage(imageUrl = badgeUrl, contentDescription = name, modifier = Modifier.size(42.dp))
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onOpen) { Text("Profile") }
        TextButton(onClick = onToggleFavorite) { Text(if (isFavorite) "Remove" else "Favorite") }
    }
}

@Composable
private fun ReminderActions(
    match: MatchInfo,
    onScheduleReminder: (MatchInfo, Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onScheduleReminder(match, 30) },
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("30 min.")
        }
        OutlinedButton(
            onClick = { onScheduleReminder(match, 0) },
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("At start")
        }
    }
}

@Composable
private fun NewsCard(article: NewsArticle) {
    val uriHandler = LocalUriHandler.current
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(article.link) },
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            article.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (article.description.isNotBlank()) {
                    Text(
                        text = article.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = listOf(article.source, article.publishedAt).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FavoriteTeamCard(
    team: FavoriteTeam,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BadgeImage(imageUrl = team.badgeUrl, contentDescription = team.name, modifier = Modifier.size(52.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = team.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = team.sport.ifBlank { "Sport not specified" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onOpen) { Text("Open") }
            TextButton(onClick = onRemove) { Text("Remove") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamProfile(
    team: TeamInfo,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val heroUrl = team.bannerUrl ?: team.badgeUrl
            if (!heroUrl.isNullOrBlank()) {
                AsyncImage(
                    model = heroUrl,
                    contentDescription = team.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)),
                    contentScale = ContentScale.Crop,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BadgeImage(imageUrl = team.badgeUrl, contentDescription = team.name)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = team.league.ifBlank { team.sport },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Country", team.country.ifBlank { "N/A" })
                StatPill("Stadium", team.stadium.ifBlank { "N/A" })
                StatPill("Founded", team.formedYear.ifBlank { "N/A" })
            }
            Button(onClick = onToggleFavorite, shape = RoundedCornerShape(8.dp)) {
                Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites")
            }
            team.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 7,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MatchHeroImage(match: MatchInfo) {
    val imageUrl = match.imageUrl
    if (imageUrl.isNullOrBlank()) {
        TeamBadgeRow(match = match)
        return
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = "Match image ${match.title}",
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun TeamBadgeRow(match: MatchInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BadgeImage(
            imageUrl = match.homeBadgeUrl ?: match.leagueBadgeUrl,
            contentDescription = match.homeTeam,
        )
        Text(
            text = "VS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BadgeImage(
            imageUrl = match.awayBadgeUrl ?: match.leagueBadgeUrl,
            contentDescription = match.awayTeam,
        )
    }
}

@Composable
private fun BadgeImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Text(
                text = contentDescription.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .heightIn(min = 56.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MatchCard(
    match: MatchInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MatchThumbnail(match = match)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = listOf(match.shortDateTime(), match.venue.ifBlank { match.country })
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MatchThumbnail(match: MatchInfo) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = match.imageUrl
        if (imageUrl.isNullOrBlank()) {
            Text(
                text = match.score ?: "VS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Image ${match.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun LiveDot() {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary),
    )
}

@Composable
private fun LoadingPanel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyPanel() {
    EmptyText("The API did not return upcoming events for this section. Choose another sport or refresh later.")
}

@Composable
private fun EmptyText(message: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Loading Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Retry")
            }
        }
    }
}
