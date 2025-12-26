package com.example.scorewatcher.domain.repository

import com.example.scorewatcher.domain.model.Match

interface ScoreSource {
    val id: String
    suspend fun fetchSnapshot(request: SnapshotRequest): List<Match>
}
