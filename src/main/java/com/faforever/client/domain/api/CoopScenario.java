package com.faforever.client.domain.api;

import com.faforever.client.coop.CoopFaction;
import com.faforever.client.coop.CoopType;
import java.util.List;

public record CoopScenario (
  Integer id,
  String name,
  String description,
  Integer order,
  CoopFaction faction,
  CoopType type,
  List<CoopMission> coopMissions
) {
  public CoopScenario {
    coopMissions = coopMissions == null ? List.of() : List.copyOf(coopMissions);
  }
}
