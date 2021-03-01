package com.faforever.client.map.management;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum MapFilter {
  CUSTOM_MAPS("management.maps.customMaps"),
  OFFICIAL_MAPS("management.maps.officialMaps"),
  ALL_MAPS("management.maps.allMaps");

  @Getter
  private final String i18n;
}
