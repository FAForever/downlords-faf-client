package com.faforever.client.filter.function;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;

import java.util.List;
import java.util.function.BiFunction;

public class FeaturedModFilterFunction implements BiFunction<List<FeaturedModBean>, GameBean, Boolean> {
  @Override
  public Boolean apply(List<FeaturedModBean> selectedMods, GameBean game) {
    return selectedMods.isEmpty() || selectedMods.stream().anyMatch(mod -> mod.getTechnicalName().equals(game.getFeaturedMod()));
  }
}
