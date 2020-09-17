package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class GameFilterPrefs {
  private final BooleanProperty showPasswordProtectedGames;
  private final BooleanProperty showModdedGames;
  private final BooleanProperty showBlacklistedMaps;
  private final SetProperty<String> mapBlacklist;
  private final SetProperty<String> mapFilters;
  private final SetProperty<String> modBlacklist;

  public GameFilterPrefs() {
    showPasswordProtectedGames = new SimpleBooleanProperty(true);
    showModdedGames = new SimpleBooleanProperty(true);
    showBlacklistedMaps = new SimpleBooleanProperty(false);
    mapBlacklist = new SimpleSetProperty<>(FXCollections.observableSet());
    mapFilters = new SimpleSetProperty<>(FXCollections.observableSet());
    modBlacklist = new SimpleSetProperty<>(FXCollections.observableSet());
  }

  public SetProperty<String> mapBlacklistProperty() {
    return mapBlacklist;
  }

  public SetProperty<String> mapFiltersProperty() {
    return mapFilters;
  }

  public ObservableSet<String> modBlacklistProperty() {
    return modBlacklist;
  }

  public BooleanProperty showPasswordProtectedGamesProperty() {
    return showPasswordProtectedGames;
  }

  public BooleanProperty showModdedGamesProperty() {
    return showModdedGames;
  }

  public BooleanProperty showBlacklistedMapsProperty() {
    return showBlacklistedMaps;
  }

  public boolean isShowPasswordProtectedGames() {
    return showPasswordProtectedGames.get();
  }

  public void setShowPasswordProtectedGames(boolean showPasswordProtectedGames) {
    this.showPasswordProtectedGames.set(showPasswordProtectedGames);
  }

  public boolean isShowModdedGames() {
    return showModdedGames.get();
  }

  public void setShowModdedGames(boolean showModdedGames) {
    this.showModdedGames.set(showModdedGames);
  }

  public boolean isShowBlacklistedMaps() {
    return showBlacklistedMaps.get();
  }

  public void setShowBlacklistedMaps(boolean showBlacklistedMaps) {
    this.showBlacklistedMaps.set(showBlacklistedMaps);
  }
}
