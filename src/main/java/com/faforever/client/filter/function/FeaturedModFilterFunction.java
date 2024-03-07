package com.faforever.client.filter.function;

import com.faforever.client.domain.api.FeaturedMod;
import com.faforever.client.domain.server.GameInfo;

import java.util.List;
import java.util.function.BiFunction;

public class FeaturedModFilterFunction implements BiFunction<List<FeaturedMod>, GameInfo, Boolean> {
  @Override
  public Boolean apply(List<FeaturedMod> selectedMods, GameInfo game) {
    return selectedMods.isEmpty() || selectedMods.stream()
                                                 .anyMatch(mod -> mod.technicalName().equals(game.getFeaturedMod()));
  }
}
