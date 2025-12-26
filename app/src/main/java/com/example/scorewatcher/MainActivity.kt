package com.example.scorewatcher

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scorewatcher.data.flashscore.FlashscoreParser
import com.example.scorewatcher.data.flashscore.FlashscoreScoreSource
import com.example.scorewatcher.data.network.OkHttpHtmlFetcher
import com.example.scorewatcher.data.repository.DefaultScoresRepository
import com.example.scorewatcher.domain.model.Match
import com.example.scorewatcher.domain.model.MatchStatus
import com.example.scorewatcher.domain.repository.MatchListMode
import com.example.scorewatcher.ui.ScoresViewModel
import com.example.scorewatcher.ui.theme.ScoreWatcherTheme
import okhttp3.OkHttpClient
import androidx.compose.foundation.clickable

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val client = OkHttpClient.Builder().build()
        val fetcher = OkHttpHtmlFetcher(client)
        val parser = FlashscoreParser()
        val flashscore = FlashscoreScoreSource(fetcher, parser)
        val repo = DefaultScoresRepository(listOf(flashscore))

        val vm = ViewModelProvider(
            this,
            VmFactory { ScoresViewModel(repo) }
        ).get(ScoresViewModel::class.java)

        setContent {
            ScoreWatcherTheme {
                ScoreWatcherApp(vm)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("Lifecycle", "MainActivity onStart")
    }
}

class VmFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    override fun <R : ViewModel> create(modelClass: Class<R>): R = creator() as R
}

@Composable
fun ScoreWatcherApp(vm: ScoresViewModel) {

    val ui by vm.state.collectAsState()

    val tabs = listOf(
        MatchListMode.LIVE to "Live",
        MatchListMode.ALL to "All",
        MatchListMode.FINISHED to "Finished"
    )
    val selectedIndex = tabs.indexOfFirst { it.first == ui.mode }.let { if (it < 0) 0 else it }

    var isMenuExpanded by remember { mutableStateOf(false) }

    var selectedMatch by remember { mutableStateOf<Match?>(null) }

    val filteredMatches = remember(ui.matches, ui.query) {
        val q = ui.query.trim().lowercase()
        if (q.isEmpty()) ui.matches
        else ui.matches.filter { m ->
            m.tournament.lowercase().contains(q) ||
                    m.homeTeam.lowercase().contains(q) ||
                    m.awayTeam.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            Column {
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ScoreWatcher",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Box {
                            IconButton(onClick = { isMenuExpanded = true }) {
                                Text("⋮")
                            }
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Настройки") },
                                    onClick = {
                                        isMenuExpanded = false
                                        // TODO: позже сделаем экран настроек
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("О приложении") },
                                    onClick = { isMenuExpanded = false }
                                )
                            }
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedIndex) {
                    tabs.forEachIndexed { index, (mode, title) ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { vm.setMode(mode) },
                            text = { Text(title) }
                        )
                    }
                }

                selectedMatch?.let { match ->
                    MatchDetailsDialog(
                        match = match,
                        onDismiss = { selectedMatch = null }
                    )
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            // Фильтр
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::setQuery,
                label = { Text("Фильтр по команде / турниру") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Автообнова", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = ui.autoRefresh,
                        onCheckedChange = vm::setAutoRefresh
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Каждые ${ui.intervalSec}с")
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { vm.setIntervalSec(ui.intervalSec - 5) }) { Text("−") }
                    IconButton(onClick = { vm.setIntervalSec(ui.intervalSec + 5) }) { Text("+") }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = vm::refreshOnce,
                    enabled = !ui.loading
                ) { Text("Обновить") }

                Spacer(Modifier.width(12.dp))

                if (ui.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            if (ui.error != null) {
                Text(
                    text = "Ошибка: ${ui.error}",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
            }

            if (filteredMatches.isEmpty() && !ui.loading && ui.error == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Пусто. Либо матчей нет, либо фильтр слишком жёсткий 🙂")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    items(
                        items = filteredMatches,
                        key = { it.id.source + ":" + it.id.sourceMatchId } // стабильный ключ
                    ) { match ->
                        CompactMatchRow(
                            match = match,
                            onClick = { selectedMatch = match }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(match: Match) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = match.tournament,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))

            val status = when (val s = match.status) {
                is MatchStatus.Live -> "LIVE ${s.minuteOrText.orEmpty()}".trim()
                is MatchStatus.Scheduled -> "⏱ ${s.startTime.orEmpty()}".trim()
                MatchStatus.Finished -> "FT"
                MatchStatus.Unknown -> "?"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${match.homeTeam} — ${match.awayTeam}")
                Text("${match.score.raw}  $status")
            }
        }
    }
}

@Composable
private fun CompactMatchRow(
    match: Match,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = "${match.homeTeam} — ${match.awayTeam}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Text(match.score.raw.ifBlank { "–" })
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun MatchDetailsDialog(
    match: Match,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Ок") }
        },
        title = { Text("${match.homeTeam} — ${match.awayTeam}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Счёт: ${match.score.raw.ifBlank { "–" }}")
                Text("Турнир: ${match.tournament}")
                Text("Статус: ${formatStatus(match.status)}")
                Text("Источник: ${match.id.source}")
                Text("ID матча: ${match.id.sourceMatchId}")
                Text("URL: ${match.detailsUrl}")
            }
        }
    )
}

private fun formatStatus(status: MatchStatus): String = when (status) {
    is MatchStatus.Live -> "LIVE ${status.minuteOrText.orEmpty()}".trim()
    is MatchStatus.Scheduled -> "⏱ ${status.startTime.orEmpty()}".trim()
    MatchStatus.Finished -> "FT"
    MatchStatus.Unknown -> "?"
}

