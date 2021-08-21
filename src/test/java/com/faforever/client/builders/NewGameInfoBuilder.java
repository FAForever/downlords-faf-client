package com.faforever.client.builders;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.commons.lobby.GameVisibility;

import java.util.HashSet;
import java.util.Set;

public class NewGameInfoBuilder {

  private final NewGameInfo newGameInfo;

  private NewGameInfoBuilder() {
    newGameInfo = new NewGameInfo();
  }

  public static NewGameInfoBuilder create() {
    return new NewGameInfoBuilder();
  }

  public NewGameInfoBuilder defaultValues() {
    map("map");
    featuredMod(FeaturedModBeanBuilder.create().defaultValues().get());
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
    newGameInfo.setTitle(title);
    return this;
  }

  public NewGameInfoBuilder password(String password) {
    newGameInfo.setPassword(password);
    return this;
  }

  public NewGameInfoBuilder featuredMod(FeaturedModBean featuredMod) {
    newGameInfo.setFeaturedMod(featuredMod);
    return this;
  }

  public NewGameInfoBuilder map(String map) {
    newGameInfo.setMap(map);
    return this;
  }

  public NewGameInfoBuilder simMods(Set<String> simMods) {
    newGameInfo.setSimMods(simMods);
    return this;
  }

  public NewGameInfoBuilder gameVisibility(GameVisibility gameVisibility) {
    newGameInfo.setGameVisibility(gameVisibility);
    return this;
  }

  public NewGameInfoBuilder ratingMin(Integer ratingMin) {
    newGameInfo.setRatingMin(ratingMin);
    return this;
  }

  public NewGameInfoBuilder ratingMax(Integer ratingMax) {
    newGameInfo.setRatingMax(ratingMax);
    return this;
  }

  public NewGameInfoBuilder enforceRatingRange(Boolean enforceRatingRange) {
    newGameInfo.setEnforceRatingRange(enforceRatingRange);
    return this;
  }

  public NewGameInfo get() {
    return newGameInfo;
  }


}
