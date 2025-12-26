package com.example.scorewatcher.data.flashscore

import com.example.scorewatcher.data.network.OkHttpHtmlFetcher
import com.example.scorewatcher.domain.model.Match
import com.example.scorewatcher.domain.model.Sport
import com.example.scorewatcher.domain.repository.MatchListMode
import com.example.scorewatcher.domain.repository.ScoreSource
import com.example.scorewatcher.domain.repository.SnapshotRequest

class FlashscoreScoreSource(
    private val fetcher: OkHttpHtmlFetcher,
    private val parser: FlashscoreParser,
    private val baseUrl: String = "https://m.flashscore.ua"
) : ScoreSource {

    override val id: String = "flashscore"

    override suspend fun fetchSnapshot(request: SnapshotRequest): List<Match> {
        require(request.sport == Sport.FOOTBALL) { "MVP: только футбол" }
        val url = when (request.mode) {
            MatchListMode.ALL -> "$baseUrl/"
            MatchListMode.LIVE -> "$baseUrl/?s=2"
            MatchListMode.FINISHED -> "$baseUrl/?d=0&s=3"
        }
        val html = fetcher.get(url)
        return parser.parse(html, baseUrl, request.mode)
    }
}
