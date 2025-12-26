package com.example.scorewatcher.domain.repository

import com.example.scorewatcher.domain.model.Match
import com.example.scorewatcher.domain.model.Sport

enum class MatchListMode { ALL, LIVE, FINISHED }

data class SnapshotRequest(
    val sport: Sport,
    val mode: MatchListMode = MatchListMode.ALL
)

interface ScoresRepository {
    suspend fun getSnapshot(request: SnapshotRequest): List<Match>
}
