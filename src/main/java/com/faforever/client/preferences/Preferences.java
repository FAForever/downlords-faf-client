package com.faforever.client.preferences;

import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn.SortType;
import javafx.util.Pair;
import lombok.Getter;

import static javafx.collections.FXCollections.observableArrayList;

public class Preferences {

  public static final String DEFAULT_THEME_NAME = "default";

  private final WindowPrefs mainWindow;
  private final GeneratorPrefs generator;
  private final ForgedAlliancePrefs forgedAlliance;
  private final LoginPrefs login;
  private final ChatPrefs chat;
  private final NotificationsPrefs notification;
  private final LocalizationPrefs localization;
  private final LastGamePrefs lastGame;
  private final MatchmakerPrefs matchmaker;
  private final NewsPrefs news;
  private final DeveloperPrefs developer;
  private final VaultPrefs vault;
  private final StringProperty themeName;
  private final BooleanProperty preReleaseCheckEnabled;
  private final BooleanProperty mapAndModAutoUpdate;
  private final BooleanProperty showPasswordProtectedGames;
  private final BooleanProperty showModdedGames;
  private final ListProperty<String> ignoredNotifications;
  private final StringProperty gamesViewMode;
  private final ListProperty<Pair<String, SortType>> gameListSorting;
  private final ObjectProperty<TilesSortingOrder> gameTileSortingOrder;
  private final ObjectProperty<UnitDataBaseType> unitDataBaseType;
  private final BooleanProperty disallowJoinsViaDiscord;
  private final BooleanProperty showGameDetailsSidePane;
  private final BooleanProperty advancedIceLogEnabled;
  private final IntegerProperty cacheLifeTimeInDays;
  private final BooleanProperty gameDataCacheActivated;
  private final BooleanProperty debugLogEnabled;

  public Preferences() {
    gameTileSortingOrder = new SimpleObjectProperty<>(TilesSortingOrder.PLAYER_DES);
    chat = new ChatPrefs();
    login = new LoginPrefs();
    generator = new GeneratorPrefs();

    localization = new LocalizationPrefs();
    lastGame = new LastGamePrefs();
    mainWindow = new WindowPrefs();
    forgedAlliance = new ForgedAlliancePrefs();
    themeName = new SimpleStringProperty(DEFAULT_THEME_NAME);
    ignoredNotifications = new SimpleListProperty<>(observableArrayList());
    notification = new NotificationsPrefs();
    matchmaker = new MatchmakerPrefs();
    gamesViewMode = new SimpleStringProperty();
    news = new NewsPrefs();
    developer = new DeveloperPrefs();
    gameListSorting = new SimpleListProperty<>(observableArrayList());
    vault = new VaultPrefs();
    unitDataBaseType = new SimpleObjectProperty<>(UnitDataBaseType.SPOOKY);
    showPasswordProtectedGames = new SimpleBooleanProperty(true);
    showModdedGames = new SimpleBooleanProperty(true);
    disallowJoinsViaDiscord = new SimpleBooleanProperty();
    showGameDetailsSidePane = new SimpleBooleanProperty(false);
    advancedIceLogEnabled = new SimpleBooleanProperty(false);
    preReleaseCheckEnabled = new SimpleBooleanProperty(false);
    cacheLifeTimeInDays = new SimpleIntegerProperty(30);
    gameDataCacheActivated = new SimpleBooleanProperty(false);
    debugLogEnabled = new SimpleBooleanProperty(false);
    mapAndModAutoUpdate = new SimpleBooleanProperty(true);
  }

  public VaultPrefs getVault() {
    return vault;
  }

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

  public WindowPrefs getMainWindow() {
    return mainWindow;
  }

  public LocalizationPrefs getLocalization() {
    return localization;
  }

  public ForgedAlliancePrefs getForgedAlliance() {
    return forgedAlliance;
  }

  public LoginPrefs getLogin() {
    return login;
  }

  public ChatPrefs getChat() {
    return chat;
  }

  public NotificationsPrefs getNotification() {
    return notification;
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

  public ObservableList<Pair<String, SortType>> getGameListSorting() {
    return gameListSorting.get();
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

  public LastGamePrefs getLastGame() {
    return lastGame;
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

  public GeneratorPrefs getGenerator() {
    return generator;
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
