package com.faforever.client.preferences;

import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class VaultPrefs {
  private final ObjectProperty<SortConfig> onlineReplaySortConfig;
  private final ObjectProperty<SortConfig> mapSortConfig;

  public VaultPrefs() {
    onlineReplaySortConfig = new SimpleObjectProperty<>(new SortConfig("startTime", SortOrder.DESC));
    mapSortConfig = new SimpleObjectProperty<>(new SortConfig("latestVersion.createTime", SortOrder.DESC));
  }

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

}
