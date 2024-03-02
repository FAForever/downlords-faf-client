package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Map(
    Integer id,
    String displayName,
    int gamesPlayed, PlayerInfo author,
    boolean recommended,
    MapType mapType, MapReviewsSummary mapReviewsSummary
) {

  @RequiredArgsConstructor
  @Getter
  public enum MapType {
    SKIRMISH("skirmish"),
    COOP("campaign_coop"),
    OTHER(null);

    private static final java.util.Map<String, MapType> VALUE_MAP = Arrays.stream(values())
                                                                          .filter(
                                                                              type -> Objects.nonNull(type.getValue()))
                                                                          .collect(Collectors.toMap(MapType::getValue,
                                                                                          Function.identity()));

    private final String value;

    public static MapType fromValue(String mapType) {
      return VALUE_MAP.getOrDefault(mapType, OTHER);
    }
  }
}
