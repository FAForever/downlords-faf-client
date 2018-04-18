package com.faforever.client.game;

import com.faforever.client.mod.FeaturedMod;
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
  private FeaturedMod featuredMod;
  private String map;
  private Set<String> simMods;
  private GameVisibility gameVisibility;

  public NewGameInfo(String title, String password, FeaturedMod featuredMod, String map, Set<String> simMods) {
    this(title, password, featuredMod, map, simMods, GameVisibility.PUBLIC);
  }
}
