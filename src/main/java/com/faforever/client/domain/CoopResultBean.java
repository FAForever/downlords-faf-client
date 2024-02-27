package com.faforever.client.domain;

import java.time.Duration;

public record CoopResultBean(
    Integer id, boolean secondaryObjectives, Duration duration, int ranking, int playerCount, ReplayBean replay
) {}
