package com.example.scorewatcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scorewatcher.domain.model.Match
import com.example.scorewatcher.domain.model.Sport
import com.example.scorewatcher.domain.repository.MatchListMode
import com.example.scorewatcher.domain.repository.ScoresRepository
import com.example.scorewatcher.domain.repository.SnapshotRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScoresUiState(
    val mode: MatchListMode = MatchListMode.LIVE,
    val query: String = "",

    val autoRefresh: Boolean = true,
    val intervalSec: Int = 30,

    val loading: Boolean = false,
    val error: String? = null,
    val matches: List<Match> = emptyList(),
    val lastUpdatedMs: Long? = null
)

class ScoresViewModel(
    private val repo: ScoresRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScoresUiState())
    val state: StateFlow<ScoresUiState> = _state.asStateFlow()

    private val refreshMutex = Mutex()
    private var autoJob: Job? = null

    init {
        refreshOnce()
        restartAutoRefresh()
    }

    fun setMode(mode: MatchListMode) {
        _state.value = _state.value.copy(mode = mode)
        refreshOnce()
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun setAutoRefresh(enabled: Boolean) {
        _state.value = _state.value.copy(autoRefresh = enabled)
        restartAutoRefresh()
    }

    fun setIntervalSec(sec: Int) {
        _state.value = _state.value.copy(intervalSec = sec.coerceIn(5, 3600))
        restartAutoRefresh()
    }

    fun refreshOnce() {
        viewModelScope.launch { refreshInternal() }
    }

    private fun restartAutoRefresh() {
        autoJob?.cancel()
        if (!_state.value.autoRefresh) return

        autoJob = viewModelScope.launch {
            while (isActive) {
                delay(_state.value.intervalSec * 1000L)
                refreshInternal()
            }
        }
    }

    private suspend fun refreshInternal() {
        refreshMutex.withLock {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val req = SnapshotRequest(
                    sport = Sport.FOOTBALL,
                    mode = _state.value.mode
                )
                val list = repo.getSnapshot(req)

                _state.value = _state.value.copy(
                    loading = false,
                    matches = list,
                    lastUpdatedMs = System.currentTimeMillis()
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = t.message ?: t::class.java.simpleName
                )
            }
        }
    }
}
