package com.faforever.client.domain;

import java.time.OffsetDateTime;

public record VotingSubjectBean(
    Integer id,
    String subject,
    String subjectKey,
    String description,
    String descriptionKey,
    String topicUrl,
    OffsetDateTime beginOfVoteTime,
    OffsetDateTime endOfVoteTime,
    int minGamesToVote,
    boolean revealWinner
) {}
