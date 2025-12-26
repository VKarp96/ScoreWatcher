package com.example.scorewatcher.data.flashscore

import com.example.scorewatcher.domain.model.*
import com.example.scorewatcher.domain.repository.MatchListMode
import org.jsoup.Jsoup

class FlashscoreParser {

    fun parse(html: String, baseUrl: String, mode: MatchListMode): List<Match> {
        val doc = Jsoup.parse(html)
        val out = mutableListOf<Match>()

        var currentTournament = ""

        doc.select("h4, a[href^=/match/]").forEach { el ->
            if (el.tagName() == "h4") {
                currentTournament = el.text().trim()
                return@forEach
            }

            val href = el.attr("href").trim()
            val scoreRaw = el.text().trim()

            val containerText = el.parent()?.text()?.trim().orEmpty()
            val parsed = parseTeamsAndStatus(containerText) ?: return@forEach
            val (home, away, status) = parsed

            val matchId = href.trim('/').split('/').getOrNull(1) ?: href
            val score = parseScore(scoreRaw)

            out += Match(
                id = MatchId("flashscore", matchId),
                sport = Sport.FOOTBALL,
                tournament = currentTournament,
                homeTeam = home,
                awayTeam = away,
                status = when {
                    mode == MatchListMode.FINISHED -> MatchStatus.Finished
                    status.kind == Kind.LIVE -> MatchStatus.Live(status.token)
                    status.kind == Kind.SCHEDULED -> MatchStatus.Scheduled(status.token)
                    else -> MatchStatus.Unknown
                },
                score = score,
                detailsUrl = baseUrl + href
            )
        }

        return out
    }

    private fun parseScore(raw: String): Score {
        val m = Regex("^(\\d+):(\\d+)").find(raw)
        val home = m?.groupValues?.getOrNull(1)?.toIntOrNull()
        val away = m?.groupValues?.getOrNull(2)?.toIntOrNull()
        return Score(home, away, raw)
    }

    private enum class Kind { LIVE, SCHEDULED, UNKNOWN }
    private data class StatusToken(val kind: Kind, val token: String?)
    private data class Parsed(val home: String, val away: String, val status: StatusToken)

    private fun parseTeamsAndStatus(text: String): Parsed? {
        val idx = text.indexOf(" - ")
        if (idx < 0) return null

        val left = text.substring(0, idx).trim()
        val right = text.substring(idx + 3).trim()

        val (token, home) = splitTokenAndTeam(left)
        val status = when {
            token.matches(Regex("\\d{1,2}:\\d{2}")) -> StatusToken(Kind.SCHEDULED, token)
            token.endsWith("'") -> StatusToken(Kind.LIVE, token)
            token.equals("Перерва", ignoreCase = true) -> StatusToken(Kind.LIVE, "Перерва")
            else -> StatusToken(Kind.UNKNOWN, null)
        }

        return Parsed(home = home, away = right, status = status)
    }

    private fun splitTokenAndTeam(left: String): Pair<String, String> {
        val apos = left.indexOf('\'')
        if (apos in 1..4 && left.length > apos + 1 && left[apos + 1].isLetter()) {
            val token = left.substring(0, apos + 1)
            val team = left.substring(apos + 1).trim()
            return token to team
        }
        val parts = left.split(" ", limit = 2)
        val token = parts.firstOrNull().orEmpty()
        val team = parts.getOrNull(1).orEmpty().trim()
        return token to team
    }
}
