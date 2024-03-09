package com.faforever.client.domain.api;

public record VotingChoice(
    String choiceText, String choiceTextKey, String description, String descriptionKey, int numberOfAnswers, int ordinal
) {}
