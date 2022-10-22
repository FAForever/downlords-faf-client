package com.faforever.client.filter.function;

import com.faforever.client.domain.GameBean;

import java.util.function.BiFunction;

public class SimModsFilterFunction implements BiFunction<Boolean, GameBean, Boolean> {
  @Override
  public Boolean apply(Boolean selected, GameBean game) {
    return !selected || game.getSimMods()
        .isEmpty();
  }
}
