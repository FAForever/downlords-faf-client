package com.faforever.client.domain.api;

public record LeagueScoreJournal(
    int id,
    int gameId,
    int loginId,
    int gameCount,
    int scoreBefore,
    int scoreAfter,
    LeagueSeason season,
    Subdivision divisionBefore,
    Subdivision divisionAfter
) {}

