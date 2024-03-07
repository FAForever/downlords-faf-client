package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.commons.api.dto.ModerationReportStatus;

import java.time.OffsetDateTime;
import java.util.Set;

public record ModerationReport(
    Integer id,
    String reportDescription,
    ModerationReportStatus reportStatus,
    String gameIncidentTimeCode,
    String moderatorNotice, PlayerInfo lastModerator, PlayerInfo reporter, Set<PlayerInfo> reportedUsers, Replay game,
    OffsetDateTime createTime
) {}
