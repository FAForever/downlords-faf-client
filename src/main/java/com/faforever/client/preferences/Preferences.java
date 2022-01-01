package com.faforever.client.preferences;

import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
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
import lombok.Getter;
import lombok.Value;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.observableHashMap;

@Value
public class Preferences {

  public static final String DEFAULT_THEME_NAME = "default";

  WindowPrefs mainWindow = new WindowPrefs();
  GeneratorPrefs generator = new GeneratorPrefs();
  ForgedAlliancePrefs forgedAlliance = new ForgedAlliancePrefs();
  LoginPrefs login = new LoginPrefs();
  ChatPrefs chat = new ChatPrefs();
  NotificationsPrefs notification = new NotificationsPrefs();
  LocalizationPrefs localization = new LocalizationPrefs();
  LastGamePrefs lastGame = new LastGamePrefs();
  MatchmakerPrefs matchmaker = new MatchmakerPrefs();
  NewsPrefs news = new NewsPrefs();
  DeveloperPrefs developer = new DeveloperPrefs();
  VaultPrefs vault = new VaultPrefs();
  StringProperty themeName = new SimpleStringProperty(DEFAULT_THEME_NAME);
  BooleanProperty preReleaseCheckEnabled = new SimpleBooleanProperty(false);
  BooleanProperty mapAndModAutoUpdate = new SimpleBooleanProperty(true);
  BooleanProperty showPasswordProtectedGames = new SimpleBooleanProperty(true);
  BooleanProperty showModdedGames = new SimpleBooleanProperty(true);
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

  public BooleanProperty showPasswordProtectedGamesProperty() {
    return showPasswordProtectedGames;
  }

  public BooleanProperty showModdedGamesProperty() {
    return showModdedGames;
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

  public MatchmakerPrefs getMatchmaker() {
    return matchmaker;
  }

  public NewsPrefs getNews() {
    return news;
  }

  public DeveloperPrefs getDeveloper() {
    return developer;
  }

  public ObservableMap<String, SortType> getGameTableSorting() {
    return gameTableSorting.get();
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

  public boolean getPreReleaseCheckEnabled() {
    return preReleaseCheckEnabled.get();
  }

  public void setPreReleaseCheckEnabled(boolean preReleaseCheckEnabled) {
    this.preReleaseCheckEnabled.set(preReleaseCheckEnabled);
  }

  public BooleanProperty preReleaseCheckEnabledProperty() {
    return preReleaseCheckEnabled;
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

  public enum UnitDataBaseType {
    SPOOKY("unitDatabase.spooky"),
    RACKOVER("unitDatabase.rackover");

    @Getter
    private final String i18nKey;

    UnitDataBaseType(String i18nKey) {
      this.i18nKey = i18nKey;
    }
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

  public boolean getMapAndModAutoUpdate() {
    return mapAndModAutoUpdate.get();
  }

  public void setMapAndModAutoUpdate(boolean mapAndModAutoUpdate) {
    this.mapAndModAutoUpdate.set(mapAndModAutoUpdate);
  }

  public BooleanProperty mapAndModAutoUpdateProperty() {
    return mapAndModAutoUpdate;
  }
}
