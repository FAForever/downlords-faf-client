package com.faforever.client.domain.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ModType {
  UI("modType.ui"), SIM("modType.sim");

  private final String i18nKey;
}
