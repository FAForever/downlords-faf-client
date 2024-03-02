package com.faforever.client.domain;

import com.faforever.commons.api.dto.ModerationReportStatus;

import java.time.OffsetDateTime;
import java.util.Set;

public record ModerationReportBean(
    Integer id,
    String reportDescription,
    ModerationReportStatus reportStatus,
    String gameIncidentTimeCode,
    String moderatorNotice,
    PlayerBean lastModerator,
    PlayerBean reporter,
    Set<PlayerBean> reportedUsers,
    ReplayBean game,
    OffsetDateTime createTime
) {}
