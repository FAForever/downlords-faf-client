package com.faforever.client.filter.function;

import com.faforever.client.domain.server.GameInfo;

import java.util.function.BiFunction;

public class SimModsFilterFunction implements BiFunction<Boolean, GameInfo, Boolean> {
  @Override
  public Boolean apply(Boolean selected, GameInfo game) {
    return !selected || game.getSimMods()
        .isEmpty();
  }
}
