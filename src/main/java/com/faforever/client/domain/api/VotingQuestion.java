package com.faforever.client.domain.api;

import java.util.List;

public record VotingQuestion(
    String question,
    String questionKey,
    String description,
    String descriptionKey,
    int maxAnswers,
    int ordinal,
    boolean alternativeQuestion,
    VotingSubject votingSubject,
    List<VotingChoice> winners,
    List<VotingChoice> votingChoices
) {

  public VotingQuestion {
    winners = winners == null ? List.of() : List.copyOf(winners);
    votingChoices = votingChoices == null ? List.of() : List.copyOf(votingChoices);
  }
}
