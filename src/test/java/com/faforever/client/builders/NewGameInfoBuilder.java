package com.faforever.client.builders;

import com.faforever.client.game.NewGameInfo;
import com.faforever.commons.lobby.GameVisibility;

import java.util.HashSet;
import java.util.Set;

public class NewGameInfoBuilder {

  private String title;
  private String password;
  private String featuredModName;
  private String map;
  private Set<String> simMods;
  private GameVisibility gameVisibility;
  private Integer ratingMin;
  private Integer ratingMax;
  private Boolean enforceRatingRange;

  public static NewGameInfoBuilder create() {
    return new NewGameInfoBuilder();
  }

  public NewGameInfoBuilder defaultValues() {
    map("map");
    featuredModName("FAF");
    password("password");
    title("title");
    simMods(new HashSet<>() {{
      add("111-456-789");
      add("222-456-789");
      add("333-456-789");
    }});
    gameVisibility(GameVisibility.PUBLIC);
    return this;
  }

  public NewGameInfoBuilder title(String title) {
    this.title = title;
    return this;
  }

  public NewGameInfoBuilder password(String password) {
    this.password = password;
    return this;
  }

  public NewGameInfoBuilder featuredModName(String featuredModName) {
    this.featuredModName = featuredModName;
    return this;
  }

  public NewGameInfoBuilder map(String map) {
    this.map = map;
    return this;
  }

  public NewGameInfoBuilder simMods(Set<String> simMods) {
    this.simMods = simMods;
    return this;
  }

  public NewGameInfoBuilder gameVisibility(GameVisibility gameVisibility) {
    this.gameVisibility = gameVisibility;
    return this;
  }

  public NewGameInfoBuilder ratingMin(Integer ratingMin) {
    this.ratingMin = ratingMin;
    return this;
  }

  public NewGameInfoBuilder ratingMax(Integer ratingMax) {
    this.ratingMax = ratingMax;
    return this;
  }

  public NewGameInfoBuilder enforceRatingRange(Boolean enforceRatingRange) {
    this.enforceRatingRange = enforceRatingRange;
    return this;
  }

  public NewGameInfo get() {
    return new NewGameInfo(title, password, featuredModName, map, simMods, gameVisibility, ratingMin, ratingMax,
                           enforceRatingRange);
  }


}
