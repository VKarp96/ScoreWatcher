package com.example.scorewatcher.data.repository

import com.example.scorewatcher.domain.model.Match
import com.example.scorewatcher.domain.repository.ScoreSource
import com.example.scorewatcher.domain.repository.ScoresRepository
import com.example.scorewatcher.domain.repository.SnapshotRequest

class DefaultScoresRepository(
    private val sources: List<ScoreSource>
) : ScoresRepository {

    override suspend fun getSnapshot(request: SnapshotRequest): List<Match> {
        return sources.flatMap { it.fetchSnapshot(request) }
    }
}
