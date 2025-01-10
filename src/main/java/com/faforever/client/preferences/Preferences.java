package com.faforever.client.preferences;

import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import com.fasterxml.jackson.annotation.JsonMerge;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableMap;
import javafx.scene.control.TableColumn.SortType;
import lombok.Getter;

import java.util.Map;

import static javafx.collections.FXCollections.observableHashMap;


public class Preferences {

  public static final String DEFAULT_THEME_NAME = "default";

  @JsonMerge
  @Getter
  private final GeneralPrefs general = new GeneralPrefs();
  @JsonMerge
  @Getter
  private final DataPrefs data = new DataPrefs();
  @Getter
  @JsonMerge
  private final WindowPrefs mainWindow = new WindowPrefs();
  @JsonMerge
  @Getter
  private final GeneratorPrefs generator = new GeneratorPrefs();
  @JsonMerge
  @Getter
  private final ForgedAlliancePrefs forgedAlliance = new ForgedAlliancePrefs();
  @JsonMerge
  @Getter
  private final LoginPrefs login = new LoginPrefs();
  @JsonMerge
  @Getter
  private final ChatPrefs chat = new ChatPrefs();
  @JsonMerge
  @Getter
  private final NotificationPrefs notification = new NotificationPrefs();
  @JsonMerge
  @Getter
  private final LocalizationPrefs localization = new LocalizationPrefs();
  @JsonMerge
  @Getter
  private final LastGamePrefs lastGame = new LastGamePrefs();
  @JsonMerge
  @Getter
  private final MatchmakerPrefs matchmaker = new MatchmakerPrefs();
  @JsonMerge
  @Getter
  private final DeveloperPrefs developer = new DeveloperPrefs();
  @JsonMerge
  @Getter
  private final VaultPrefs vault = new VaultPrefs();
  @JsonMerge
  @Getter
  private final UserPrefs user = new UserPrefs();
  @JsonMerge
  @Getter
  private final FiltersPrefs filters = new FiltersPrefs();

  /* Temporary solution to see POC, probably will be moved somewhere else and renamed*/
  @JsonMerge
  @Getter
  ReplayHistoryPrefs replayHistory = new ReplayHistoryPrefs();

  private final StringProperty themeName = new SimpleStringProperty(DEFAULT_THEME_NAME);
  private final BooleanProperty preReleaseCheckEnabled = new SimpleBooleanProperty(false);
  private final BooleanProperty mapAndModAutoUpdate = new SimpleBooleanProperty(true);
  private final BooleanProperty hidePrivateGames = new SimpleBooleanProperty(false);
  private final BooleanProperty hideModdedGames = new SimpleBooleanProperty(false);
  private final StringProperty gamesViewMode = new SimpleStringProperty();
  private final MapProperty<String, SortType> gameTableSorting = new SimpleMapProperty<>(observableHashMap());
  private final ObjectProperty<TilesSortingOrder> gameTileSortingOrder = new SimpleObjectProperty<>(
      TilesSortingOrder.PLAYER_DES);
  private final ObjectProperty<UnitDataBaseType> unitDataBaseType = new SimpleObjectProperty<>(UnitDataBaseType.SPOOKY);
  private final BooleanProperty disallowJoinsViaDiscord = new SimpleBooleanProperty();
  private final BooleanProperty showGameDetailsSidePane = new SimpleBooleanProperty(false);
  private final BooleanProperty advancedIceLogEnabled = new SimpleBooleanProperty(false);
  private final IntegerProperty cacheLifeTimeInDays = new SimpleIntegerProperty(30);
  private final BooleanProperty gameDataCacheActivated = new SimpleBooleanProperty(false);


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
