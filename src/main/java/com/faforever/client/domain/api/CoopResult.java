package com.faforever.client.domain.api;

import java.time.Duration;

public record CoopResult(
    Integer id, boolean secondaryObjectives, Duration duration, int ranking, int playerCount, Replay replay
) {}
