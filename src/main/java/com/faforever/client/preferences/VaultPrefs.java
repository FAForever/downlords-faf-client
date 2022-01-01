package com.faforever.client.preferences;

import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import lombok.Value;

@Value
public class VaultPrefs {
  ObjectProperty<SortConfig> onlineReplaySortConfig = new SimpleObjectProperty<>(new SortConfig("startTime", SortOrder.DESC));
  ObjectProperty<SortConfig> mapSortConfig = new SimpleObjectProperty<>(new SortConfig("gamesPlayed", SortOrder.DESC));
  ObjectProperty<SortConfig> modVaultConfig = new SimpleObjectProperty<>(new SortConfig("latestVersion.createTime", SortOrder.DESC));
  MapProperty<String, String> savedReplayQueries = new SimpleMapProperty<>(FXCollections.observableHashMap());
  MapProperty<String, String> savedMapQueries = new SimpleMapProperty<>(FXCollections.observableHashMap());
  MapProperty<String, String> savedModQueries = new SimpleMapProperty<>(FXCollections.observableHashMap());

  public SortConfig getOnlineReplaySortConfig() {
    return onlineReplaySortConfig.get();
  }

  public void setOnlineReplaySortConfig(SortConfig onlineReplaySortConfig) {
    this.onlineReplaySortConfig.set(onlineReplaySortConfig);
  }

  public ObjectProperty<SortConfig> onlineReplaySortConfigProperty() {
    return onlineReplaySortConfig;
  }

  public SortConfig getMapSortConfig() {
    return mapSortConfig.get();
  }

  public void setMapSortConfig(SortConfig mapSortConfig) {
    this.mapSortConfig.set(mapSortConfig);
  }

  public ObjectProperty<SortConfig> mapSortConfigProperty() {
    return mapSortConfig;
  }

  public SortConfig getModVaultConfig() {
    return modVaultConfig.get();
  }

  public void setModVaultConfig(SortConfig modVaultConfig) {
    this.modVaultConfig.set(modVaultConfig);
  }

  public ObjectProperty<SortConfig> modVaultConfigProperty() {
    return modVaultConfig;
  }

  public ObservableMap<String, String> getSavedReplayQueries() {
    return savedReplayQueries;
  }

  public void setSavedReplayQueries(ObservableMap<String, String> savedReplayQueries) {
    this.savedReplayQueries.set(savedReplayQueries);
  }

  public MapProperty<String, String> savedReplayQueriesProperty() {
    return savedReplayQueries;
  }

  public ObservableMap<String, String> getSavedMapQueries() {
    return savedMapQueries.get();
  }

  public void setSavedMapQueries(ObservableMap<String, String> savedMapQueries) {
    this.savedMapQueries.set(savedMapQueries);
  }

  public MapProperty<String, String> savedMapQueriesProperty() {
    return savedMapQueries;
  }

  public ObservableMap<String, String> getSavedModQueries() {
    return savedModQueries.get();
  }

  public void setSavedModQueries(ObservableMap<String, String> savedModQueries) {
    this.savedModQueries.set(savedModQueries);
  }

  public MapProperty<String, String> savedModQueriesProperty() {
    return savedModQueries;
  }
}
