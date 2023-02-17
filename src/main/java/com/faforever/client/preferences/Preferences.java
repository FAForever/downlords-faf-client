package com.faforever.client.preferences;

import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import com.fasterxml.jackson.annotation.JsonMerge;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.TableColumn.SortType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Map;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.observableHashMap;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class Preferences {

  public static final String DEFAULT_THEME_NAME = "default";

  @JsonMerge
  @Getter
  GeneralPrefs general = new GeneralPrefs();
  @JsonMerge
  @Getter
  DataPrefs data = new DataPrefs();
  @JsonMerge
  @Getter
  WindowPrefs window = new WindowPrefs();
  @JsonMerge
  @Getter
  GeneratorPrefs generator = new GeneratorPrefs();
  @JsonMerge
  @Getter
  ForgedAlliancePrefs forgedAlliance = new ForgedAlliancePrefs();
  @JsonMerge
  @Getter
  LoginPrefs login = new LoginPrefs();
  @JsonMerge
  @Getter
  ChatPrefs chat = new ChatPrefs();
  @JsonMerge
  @Getter
  NotificationPrefs notification = new NotificationPrefs();
  @JsonMerge
  @Getter
  LocalizationPrefs localization = new LocalizationPrefs();
  @JsonMerge
  @Getter
  LastGamePrefs lastGame = new LastGamePrefs();
  @JsonMerge
  @Getter
  MatchmakerPrefs matchmaker = new MatchmakerPrefs();
  @JsonMerge
  @Getter
  NewsPrefs news = new NewsPrefs();
  @JsonMerge
  @Getter
  DeveloperPrefs developer = new DeveloperPrefs();
  @JsonMerge
  @Getter
  VaultPrefs vault = new VaultPrefs();
  @JsonMerge
  @Getter
  UserPrefs user = new UserPrefs();
  @JsonMerge
  @Getter
  FiltersPrefs filters = new FiltersPrefs();

  StringProperty themeName = new SimpleStringProperty(DEFAULT_THEME_NAME);
  BooleanProperty preReleaseCheckEnabled = new SimpleBooleanProperty(false);
  BooleanProperty mapAndModAutoUpdate = new SimpleBooleanProperty(true);
  BooleanProperty hidePrivateGames = new SimpleBooleanProperty(false);
  BooleanProperty hideModdedGames = new SimpleBooleanProperty(false);
  ListProperty<String> ignoredNotifications = new SimpleListProperty<>(observableArrayList());
  StringProperty gamesViewMode = new SimpleStringProperty();
  MapProperty<String, SortType> gameTableSorting = new SimpleMapProperty<>(observableHashMap());
  ObjectProperty<TilesSortingOrder> gameTileSortingOrder = new SimpleObjectProperty<>(TilesSortingOrder.PLAYER_DES);
  ObjectProperty<UnitDataBaseType> unitDataBaseType = new SimpleObjectProperty<>(UnitDataBaseType.SPOOKY);
  BooleanProperty disallowJoinsViaDiscord = new SimpleBooleanProperty();
  BooleanProperty showGameDetailsSidePane = new SimpleBooleanProperty(false);
  BooleanProperty advancedIceLogEnabled = new SimpleBooleanProperty(false);
  IntegerProperty cacheLifeTimeInDays = new SimpleIntegerProperty(30);
  BooleanProperty gameDataCacheActivated = new SimpleBooleanProperty(false);
  BooleanProperty debugLogEnabled = new SimpleBooleanProperty(false);


  public TilesSortingOrder getGameTileSortingOrder() {
    return gameTileSortingOrder.get();
  }

  public void setGameTileSortingOrder(TilesSortingOrder gameTileTilesSortingOrder) {
    this.gameTileSortingOrder.set(gameTileTilesSortingOrder);
  }

  public ObjectProperty<TilesSortingOrder> gameTileSortingOrderProperty() {
    return gameTileSortingOrder;
  }

  public BooleanProperty hidePrivateGamesProperty() {
    return hidePrivateGames;
  }

  public BooleanProperty hideModdedGamesProperty() {
    return hideModdedGames;
  }

  public String getGamesViewMode() {
    return gamesViewMode.get();
  }

  public void setGamesViewMode(String gamesViewMode) {
    this.gamesViewMode.set(gamesViewMode);
  }

  public StringProperty gamesViewModeProperty() {
    return gamesViewMode;
  }

  public String getThemeName() {
    return themeName.get();
  }

  public void setThemeName(String themeName) {
    this.themeName.set(themeName);
  }

  public StringProperty themeNameProperty() {
    return themeName;
  }

  public ObservableList<String> getIgnoredNotifications() {
    return ignoredNotifications.get();
  }

  public void setIgnoredNotifications(ObservableList<String> ignoredNotifications) {
    this.ignoredNotifications.set(ignoredNotifications);
  }

  public ListProperty<String> ignoredNotificationsProperty() {
    return ignoredNotifications;
  }

  public ObservableMap<String, SortType> getGameTableSorting() {
    return gameTableSorting.get();
  }

  public void setGameTableSorting(Map<String, SortType> gameTableSorting) {
    this.gameTableSorting.clear();
    this.gameTableSorting.putAll(gameTableSorting);
  }

  public UnitDataBaseType getUnitDataBaseType() {
    return unitDataBaseType.get();
  }

  public void setUnitDataBaseType(UnitDataBaseType unitDataBaseType) {
    this.unitDataBaseType.set(unitDataBaseType);
  }

  public ObjectProperty<UnitDataBaseType> unitDataBaseTypeProperty() {
    return unitDataBaseType;
  }

  public boolean isDisallowJoinsViaDiscord() {
    return disallowJoinsViaDiscord.get();
  }

  public void setDisallowJoinsViaDiscord(boolean disallowJoinsViaDiscord) {
    this.disallowJoinsViaDiscord.set(disallowJoinsViaDiscord);
  }

  public BooleanProperty disallowJoinsViaDiscordProperty() {
    return disallowJoinsViaDiscord;
  }

  public boolean isShowGameDetailsSidePane() {
    return showGameDetailsSidePane.get();
  }

  public void setShowGameDetailsSidePane(boolean showGameDetailsSidePane) {
    this.showGameDetailsSidePane.set(showGameDetailsSidePane);
  }

  public boolean isAdvancedIceLogEnabled() {
    return advancedIceLogEnabled.get();
  }

  public void setAdvancedIceLogEnabled(boolean advancedIceLogEnabled) {
    this.advancedIceLogEnabled.set(advancedIceLogEnabled);
  }

  public BooleanProperty advancedIceLogEnabledProperty() {
    return advancedIceLogEnabled;
  }

  public BooleanProperty showGameDetailsSidePaneProperty() {
    return showGameDetailsSidePane;
  }

  public boolean isPreReleaseCheckEnabled() {
    return preReleaseCheckEnabled.get();
  }

  public void setPreReleaseCheckEnabled(boolean preReleaseCheckEnabled) {
    this.preReleaseCheckEnabled.set(preReleaseCheckEnabled);
  }

  public BooleanProperty preReleaseCheckEnabledProperty() {
    return preReleaseCheckEnabled;
  }

  public boolean isHidePrivateGames() {
    return hidePrivateGames.get();
  }

  public void setHidePrivateGames(boolean hidePrivateGames) {
    this.hidePrivateGames.set(hidePrivateGames);
  }

  public boolean isHideModdedGames() {
    return hideModdedGames.get();
  }

  public void setHideModdedGames(boolean hideModdedGames) {
    this.hideModdedGames.set(hideModdedGames);
  }

  public int getCacheLifeTimeInDays() {
    return cacheLifeTimeInDays.get();
  }

  public void setCacheLifeTimeInDays(int cacheLifeTimeInDays) {
    this.cacheLifeTimeInDays.set(cacheLifeTimeInDays);
  }

  public IntegerProperty cacheLifeTimeInDaysProperty() {
    return cacheLifeTimeInDays;
  }

  public boolean isGameDataCacheActivated() {
    return gameDataCacheActivated.get();
  }

  public void setGameDataCacheActivated(boolean gameDataCacheActivated) {
    this.gameDataCacheActivated.set(gameDataCacheActivated);
  }

  public BooleanProperty gameDataCacheActivatedProperty() {
    return gameDataCacheActivated;
  }

  public boolean isDebugLogEnabled() {
    return debugLogEnabled.get();
  }

  public void setDebugLogEnabled(boolean debugLogEnabled) {
    this.debugLogEnabled.set(debugLogEnabled);
  }

  public BooleanProperty debugLogEnabledProperty() {
    return debugLogEnabled;
  }

  public boolean isMapAndModAutoUpdate() {
    return mapAndModAutoUpdate.get();
  }

  public void setMapAndModAutoUpdate(boolean mapAndModAutoUpdate) {
    this.mapAndModAutoUpdate.set(mapAndModAutoUpdate);
  }

  public BooleanProperty mapAndModAutoUpdateProperty() {
    return mapAndModAutoUpdate;
  }

  public enum UnitDataBaseType {
    SPOOKY("unitDatabase.spooky"),
    RACKOVER("unitDatabase.rackover");

    @Getter
    private final String i18nKey;

    UnitDataBaseType(String i18nKey) {
      this.i18nKey = i18nKey;
    }
  }
}
