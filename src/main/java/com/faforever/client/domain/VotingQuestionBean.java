package com.faforever.client.domain;

import java.util.List;

public record VotingQuestionBean(
    String question,
    String questionKey,
    String description,
    String descriptionKey,
    int maxAnswers,
    int ordinal,
    boolean alternativeQuestion,
    VotingSubjectBean votingSubject,
    List<VotingChoiceBean> winners,
    List<VotingChoiceBean> votingChoices
) {

  public VotingQuestionBean {
    winners = winners == null ? List.of() : List.copyOf(winners);
    votingChoices = votingChoices == null ? List.of() : List.copyOf(votingChoices);
  }
}
