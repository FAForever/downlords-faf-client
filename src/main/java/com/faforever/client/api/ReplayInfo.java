package com.faforever.client.api;

import com.google.api.client.util.Key;
import javafx.collections.ObservableMap;

import java.time.Instant;
import java.util.List;

public class ReplayInfo {
  @Key
  private int id;
  @Key
  private String title;
  @Key("featured_mod_id")
  private String featuredModId;
  @Key("map_id")
  private String mapId;
  @Key("start_time")
  private Instant startTime;
  @Key("end_time")
  private Instant endTime;
  @Key
  private int views;
  @Key
  private ObservableMap<String, List<String>> teams;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getFeaturedModId() {
    return featuredModId;
  }

  public void setFeaturedModId(String featuredModId) {
    this.featuredModId = featuredModId;
  }

  public String getMapId() {
    return mapId;
  }

  public void setMapId(String mapId) {
    this.mapId = mapId;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
  }

  public int getViews() {
    return views;
  }

  public void setViews(int views) {
    this.views = views;
  }

  public ObservableMap<String, List<String>> getTeams() {
    return teams;
  }

  public void setTeams(ObservableMap<String, List<String>> teams) {
    this.teams = teams;
  }
}
