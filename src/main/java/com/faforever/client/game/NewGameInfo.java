package com.faforever.client.game;

import com.faforever.client.mod.FeaturedModBean;

import java.util.Set;

public class NewGameInfo {

  private String title;
  private String password;
  private FeaturedModBean featuredMod;
  private String map;
  private Set<String> simMods;

  public NewGameInfo() {
  }

  public NewGameInfo(String title, String password, FeaturedModBean featuredMod, String map, Set<String> simMods) {
    this.title = title;
    this.password = password;
    this.featuredMod = featuredMod;
    this.map = map;
    this.simMods = simMods;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getMap() {
    return map;
  }

  public void setMap(String map) {
    this.map = map;
  }

  public FeaturedModBean getFeaturedMod() {
    return featuredMod;
  }

  public void setFeaturedMod(FeaturedModBean featuredMod) {
    this.featuredMod = featuredMod;
  }

  public Set<String> getSimMods() {
    return simMods;
  }

  public void setSimMods(Set<String> simMods) {
    this.simMods = simMods;
  }
}
