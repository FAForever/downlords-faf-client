package com.faforever.client.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

public record TournamentBean(
    Integer id,
    String name,
    String description,
    String tournamentType,
    int participantCount,
    OffsetDateTime createdAt,
    OffsetDateTime startingAt,
    OffsetDateTime completedAt,
    String challongeUrl,
    String liveImageUrl,
    String signUpUrl, boolean openForSignup
) {

  public Status status() {
    if (completedAt() != null) {
        return Status.FINISHED;
    } else if (startingAt() != null && startingAt().isBefore(OffsetDateTime.now())) {
        return Status.RUNNING;
    } else if (openForSignup()) {
        return Status.OPEN_FOR_REGISTRATION;
      } else {
        return Status.CLOSED_FOR_REGISTRATION;
      }
  }

  @AllArgsConstructor
  @Getter
  public enum Status {
    FINISHED("tournament.status.finished"),
    RUNNING("tournament.status.running"),
    OPEN_FOR_REGISTRATION("tournament.status.openForRegistration"),
    CLOSED_FOR_REGISTRATION("tournament.status.closedForRegistration");

    private final String messageKey;
  }
}
