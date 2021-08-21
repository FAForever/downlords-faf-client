package com.faforever.client.game;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.commons.lobby.GameVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewGameInfo {
  private String title;
  private String password;
  private FeaturedModBean featuredMod;
  private String map;
  private Set<String> simMods;
  private GameVisibility gameVisibility;
  private Integer ratingMin;
  private Integer ratingMax;
  private Boolean enforceRatingRange;

  public NewGameInfo(String title, String password, FeaturedModBean featuredMod, String map, Set<String> simMods) {
    this(title, password, featuredMod, map, simMods, GameVisibility.PUBLIC, null, null, false);
  }
}
