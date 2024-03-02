package com.faforever.client.domain.api;

import java.time.OffsetDateTime;

public record VotingSubject(
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
