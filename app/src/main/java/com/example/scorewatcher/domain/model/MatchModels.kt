package com.example.scorewatcher.domain.model

data class MatchId(
    val source: String,        // "flashscore"
    val sourceMatchId: String  // id матча в сорсе
)

enum class Sport { FOOTBALL } // MVP

sealed class MatchStatus {
    data class Scheduled(val startTime: String?) : MatchStatus()
    data class Live(val minuteOrText: String?) : MatchStatus()
    data object Finished : MatchStatus()
    data object Unknown : MatchStatus()
}

data class Score(
    val home: Int?,
    val away: Int?,
    val raw: String
)

data class Match(
    val id: MatchId,
    val sport: Sport,
    val tournament: String,
    val homeTeam: String,
    val awayTeam: String,
    val status: MatchStatus,
    val score: Score,
    val detailsUrl: String
)
