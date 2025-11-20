package com.faforever.client.preferences;

import com.faforever.client.domain.api.MapPoolAssignment;

public record VetoKey(
    int matchmakerQueueMapPoolId,
    int mapPoolMapVersionId
) {

  public static VetoKey of(MapPoolAssignment assignment) {
    return new VetoKey(assignment.mapPool().mapPool().id(), assignment.id());
  }

}