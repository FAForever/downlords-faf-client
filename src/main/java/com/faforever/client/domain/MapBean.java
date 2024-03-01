package com.faforever.client.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public record MapBean(
    Integer id,
    String displayName,
    int gamesPlayed,
    PlayerBean author,
    boolean recommended,
    MapType mapType,
    MapReviewsSummaryBean mapReviewsSummary
) {

  @RequiredArgsConstructor
  @Getter
  public enum MapType {
    SKIRMISH("skirmish"),
    COOP("campaign_coop"),
    OTHER(null);

    private static final Map<String, MapType> VALUE_MAP = Arrays.stream(values())
                                                                .filter(type -> Objects.nonNull(type.getValue()))
                                                                .collect(Collectors.toMap(MapType::getValue,
                                                                                          Function.identity()));

    private final String value;

    public static MapType fromValue(String mapType) {
      return VALUE_MAP.getOrDefault(mapType, OTHER);
    }
  }
}
