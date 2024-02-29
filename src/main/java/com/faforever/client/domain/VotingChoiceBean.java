package com.faforever.client.domain;

public record VotingChoiceBean(
    String choiceText, String choiceTextKey, String description, String descriptionKey, int numberOfAnswers, int ordinal
) {}
