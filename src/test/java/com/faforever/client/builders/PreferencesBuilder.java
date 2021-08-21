package com.faforever.client.builders;

import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.chat.ChatFormat;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.map.generator.GenerationType;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.DateInfo;
import com.faforever.client.preferences.DeveloperPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.GeneratorPrefs;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.preferences.LocalizationPrefs;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.preferences.NewsPrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import com.faforever.client.preferences.TimeInfo;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.lobby.Faction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.paint.Color;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PreferencesBuilder {

  private final Preferences preferences;

  private PreferencesBuilder() {
    preferences = new Preferences();
  }

  public static PreferencesBuilder create() {
    return new PreferencesBuilder();
  }

  public PreferencesBuilder defaultValues() {
    return this;
  }

  public PreferencesBuilder themeName(String themeName) {
    preferences.setThemeName(themeName);
    return this;
  }

  public PreferencesBuilder preReleaseCheckEnabled(boolean preReleaseCheckEnabled) {
    preferences.setPreReleaseCheckEnabled(preReleaseCheckEnabled);
    return this;
  }

  public PreferencesBuilder showPasswordProtectedGames(boolean showPasswordProtectedGames) {
    preferences.setShowPasswordProtectedGames(showPasswordProtectedGames);
    return this;
  }

  public PreferencesBuilder showModdedGames(boolean showModdedGames) {
    preferences.setShowModdedGames(showModdedGames);
    return this;
  }

  public PreferencesBuilder ignoredNotifications(List<String> ignoredNotifications) {
    preferences.setIgnoredNotifications(FXCollections.observableList(ignoredNotifications));
    return this;
  }

  public PreferencesBuilder gamesViewMode(String gamesViewMode) {
    preferences.setGamesViewMode(gamesViewMode);
    return this;
  }

  public PreferencesBuilder gameListSorting(Map<String, SortType> gameListSorting) {
    preferences.getGameTableSorting().clear();
    preferences.getGameTableSorting().putAll(gameListSorting);
    return this;
  }

  public PreferencesBuilder gameTileSortingOrder(TilesSortingOrder gameTileSortingOrder) {
    preferences.setGameTileSortingOrder(gameTileSortingOrder);
    return this;
  }

  public PreferencesBuilder unitDataBaseType(UnitDataBaseType unitDataBaseType) {
    preferences.setUnitDataBaseType(unitDataBaseType);
    return this;
  }

  public PreferencesBuilder disallowJoinsViaDiscord(boolean disallowJoinsViaDiscord) {
    preferences.setDisallowJoinsViaDiscord(disallowJoinsViaDiscord);
    return this;
  }

  public PreferencesBuilder showGameDetailsSidePane(boolean showGameDetailsSidePane) {
    preferences.setShowGameDetailsSidePane(showGameDetailsSidePane);
    return this;
  }

  public PreferencesBuilder advancedIceLogEnabled(boolean advancedIceLogEnabled) {
    preferences.setAdvancedIceLogEnabled(advancedIceLogEnabled);
    return this;
  }

  public PreferencesBuilder cacheLifeTimeInDays(int cacheLifeTimeInDays) {
    preferences.setCacheLifeTimeInDays(cacheLifeTimeInDays);
    return this;
  }

  public PreferencesBuilder gameDataCacheActivated(boolean gameDataCacheActivated) {
    preferences.setGameDataCacheActivated(gameDataCacheActivated);
    return this;
  }

  public WindowPrefsBuilder windowPrefs() {
    return new WindowPrefsBuilder();
  }

  public GeneratorPrefsBuilder generatorPrefs() {
    return new GeneratorPrefsBuilder();
  }

  public ForgedAlliancePrefsBuilder forgedAlliancePrefs() {
    return new ForgedAlliancePrefsBuilder();
  }

  public LoginPrefsBuilder loginPrefs() {
    return new LoginPrefsBuilder();
  }

  public ChatPrefsBuilder chatPrefs() {
    return new ChatPrefsBuilder();
  }

  public NotificationsPrefsBuilder notificationsPrefs() {
    return new NotificationsPrefsBuilder();
  }

  public LocalizationPrefsBuilder localizationPrefs() {
    return new LocalizationPrefsBuilder();
  }

  public LastGamePrefsBuilder lastGamePrefs() {
    return new LastGamePrefsBuilder();
  }

  public MatchmakerPrefsBuilder matchmakerPrefs() {
    return new MatchmakerPrefsBuilder();
  }

  public NewsPrefsBuilder newsPrefs() {
    return new NewsPrefsBuilder();
  }

  public DeveloperPrefsBuilder developerPrefs() {
    return new DeveloperPrefsBuilder();
  }

  public VaultPrefsBuilder vaultPrefs() {
    return new VaultPrefsBuilder();
  }

  public Preferences get() {
    return preferences;
  }

  abstract class SubPreferencesBuilder {
    public PreferencesBuilder then() {
      return PreferencesBuilder.this;
    }
  }

  public class ChatPrefsBuilder extends SubPreferencesBuilder {
    private final ChatPrefs chatPrefs = preferences.getChat();

    public ChatPrefsBuilder zoom(Double zoom) {
      chatPrefs.setZoom(zoom);
      return this;
    }

    public ChatPrefsBuilder learnedAutoComplete(boolean learnedAutoComplete) {
      chatPrefs.setLearnedAutoComplete(learnedAutoComplete);
      return this;
    }

    public ChatPrefsBuilder previewImageUrls(boolean previewImageUrls) {
      chatPrefs.setPreviewImageUrls(previewImageUrls);
      return this;
    }

    public ChatPrefsBuilder maxMessages(int maxMessages) {
      chatPrefs.setMaxMessages(maxMessages);
      return this;
    }

    public ChatPrefsBuilder chatColorMode(ChatColorMode chatColorMode) {
      chatPrefs.setChatColorMode(chatColorMode);
      return this;
    }

    public ChatPrefsBuilder channelTabScrollPaneWidth(int channelTabScrollPaneWidth) {
      chatPrefs.setChannelTabScrollPaneWidth(channelTabScrollPaneWidth);
      return this;
    }

    public ChatPrefsBuilder userToColor(ObservableMap<String, Color> userToColor) {
      chatPrefs.setUserToColor(userToColor);
      return this;
    }

    public ChatPrefsBuilder groupToColor(ObservableMap<ChatUserCategory, Color> groupToColor) {
      chatPrefs.setGroupToColor(groupToColor);
      return this;
    }

    public ChatPrefsBuilder hideFoeMessages(boolean hideFoeMessages) {
      chatPrefs.setHideFoeMessages(hideFoeMessages);
      return this;
    }

    public ChatPrefsBuilder playerListShown(boolean playerListShown) {
      chatPrefs.setPlayerListShown(playerListShown);
      return this;
    }

    public ChatPrefsBuilder timeFormat(TimeInfo timeFormat) {
      chatPrefs.setTimeFormat(timeFormat);
      return this;
    }

    public ChatPrefsBuilder dateFormat(DateInfo dateFormat) {
      chatPrefs.setDateFormat(dateFormat);
      return this;
    }

    public ChatPrefsBuilder chatFormat(ChatFormat chatFormat) {
      chatPrefs.setChatFormat(chatFormat);
      return this;
    }

    public ChatPrefsBuilder autoJoinChannels(ObservableList<String> autoJoinChannels) {
      chatPrefs.getAutoJoinChannels().setAll(autoJoinChannels);
      return this;
    }

    public ChatPrefsBuilder idleThreshold(int idleThreshold) {
      chatPrefs.setIdleThreshold(idleThreshold);
      return this;
    }
  }

  public class DeveloperPrefsBuilder extends SubPreferencesBuilder {
    private final DeveloperPrefs developerPrefs = preferences.getDeveloper();

    public DeveloperPrefsBuilder gameRepositoryUrl(String gameRepositoryUrl) {
      developerPrefs.setGameRepositoryUrl(gameRepositoryUrl);
      return this;
    }
  }

  public class ForgedAlliancePrefsBuilder extends SubPreferencesBuilder {
    private final ForgedAlliancePrefs forgedAlliancePrefs = preferences.getForgedAlliance();

    public ForgedAlliancePrefsBuilder installationPath(Path installationPath) {
      forgedAlliancePrefs.setInstallationPath(installationPath);
      return this;
    }

    public ForgedAlliancePrefsBuilder preferencesFile(Path preferencesFile) {
      forgedAlliancePrefs.setPreferencesFile(preferencesFile);
      return this;
    }

    public ForgedAlliancePrefsBuilder vaultBaseDirectory(Path vaultBaseDirectory) {
      forgedAlliancePrefs.setVaultBaseDirectory(vaultBaseDirectory);
      return this;
    }

    public ForgedAlliancePrefsBuilder customMapsDirectory(Path customMapsDirectory) {
      forgedAlliancePrefs.customMapsDirectoryProperty().unbind();
      forgedAlliancePrefs.setCustomMapsDirectory(customMapsDirectory);
      return this;
    }

    public ForgedAlliancePrefsBuilder modsDirectory(Path modsDirectory) {
      forgedAlliancePrefs.modsDirectoryProperty().unbind();
      forgedAlliancePrefs.setModsDirectory(modsDirectory);
      return this;
    }

    public ForgedAlliancePrefsBuilder forceRelay(boolean forceRelay) {
      forgedAlliancePrefs.setForceRelay(forceRelay);
      return this;
    }

    public ForgedAlliancePrefsBuilder autoDownloadMaps(boolean autoDownloadMaps) {
      forgedAlliancePrefs.setAutoDownloadMaps(autoDownloadMaps);
      return this;
    }

    public ForgedAlliancePrefsBuilder allowReplaysWhileInGame(boolean allowReplaysWhileInGame) {
      forgedAlliancePrefs.setAllowReplaysWhileInGame(allowReplaysWhileInGame);
      return this;
    }

    public ForgedAlliancePrefsBuilder vaultCheckDone(boolean vaultCheckDone) {
      forgedAlliancePrefs.setVaultCheckDone(vaultCheckDone);
      return this;
    }

    public ForgedAlliancePrefsBuilder executableDecorator(String executableDecorator) {
      forgedAlliancePrefs.setExecutableDecorator(executableDecorator);
      return this;
    }

    public ForgedAlliancePrefsBuilder executionDirectory(Path executionDirectory) {
      forgedAlliancePrefs.setExecutionDirectory(executionDirectory);
      return this;
    }
  }

  public class GeneratorPrefsBuilder extends SubPreferencesBuilder {
    private final GeneratorPrefs generatorPrefs = preferences.getGenerator();

    public GeneratorPrefsBuilder generationType(GenerationType generationType) {
      generatorPrefs.setGenerationType(generationType);
      return this;
    }

    public GeneratorPrefsBuilder spawnCount(int spawnCount) {
      generatorPrefs.setSpawnCount(spawnCount);
      return this;
    }

    public GeneratorPrefsBuilder mapSize(String mapSize) {
      generatorPrefs.setMapSize(mapSize);
      return this;
    }

    public GeneratorPrefsBuilder waterDensity(int waterDensity) {
      generatorPrefs.setWaterDensity(waterDensity);
      return this;
    }

    public GeneratorPrefsBuilder waterRandom(boolean waterRandom) {
      generatorPrefs.setWaterRandom(waterRandom);
      return this;
    }

    public GeneratorPrefsBuilder plateauDensity(int plateauDensity) {
      generatorPrefs.setPlateauDensity(plateauDensity);
      return this;
    }

    public GeneratorPrefsBuilder plateauRandom(boolean plateauRandom) {
      generatorPrefs.setPlateauRandom(plateauRandom);
      return this;
    }

    public GeneratorPrefsBuilder mountainDensity(int mountainDensity) {
      generatorPrefs.setMountainDensity(mountainDensity);
      return this;
    }

    public GeneratorPrefsBuilder mountainRandom(boolean mountainRandom) {
      generatorPrefs.setMountainRandom(mountainRandom);
      return this;
    }

    public GeneratorPrefsBuilder rampDensity(int rampDensity) {
      generatorPrefs.setRampDensity(rampDensity);
      return this;
    }

    public GeneratorPrefsBuilder rampRandom(boolean rampRandom) {
      generatorPrefs.setRampRandom(rampRandom);
      return this;
    }
  }

  public class MatchmakerPrefsBuilder extends SubPreferencesBuilder {
    private final MatchmakerPrefs matchmakerPrefs = preferences.getMatchmaker();

    public MatchmakerPrefsBuilder factions(ObservableList<Faction> factions) {
      matchmakerPrefs.getFactions().setAll(factions);
      return this;
    }
  }

  public class LastGamePrefsBuilder extends SubPreferencesBuilder {
    private final LastGamePrefs lastGamePrefs = preferences.getLastGame();

    public LastGamePrefsBuilder lastGameType(String lastGameType) {
      lastGamePrefs.setLastGameType(lastGameType);
      return this;
    }

    public LastGamePrefsBuilder lastGameTitle(String lastGameTitle) {
      lastGamePrefs.setLastGameTitle(lastGameTitle);
      return this;
    }

    public LastGamePrefsBuilder lastMap(String lastMap) {
      lastGamePrefs.setLastMap(lastMap);
      return this;
    }

    public LastGamePrefsBuilder lastGamePassword(String lastGamePassword) {
      lastGamePrefs.setLastGamePassword(lastGamePassword);
      return this;
    }

    public LastGamePrefsBuilder lastGameMinRating(Integer lastGameMinRating) {
      lastGamePrefs.setLastGameMinRating(lastGameMinRating);
      return this;
    }

    public LastGamePrefsBuilder lastGameMaxRating(Integer lastGameMaxRating) {
      lastGamePrefs.setLastGameMaxRating(lastGameMaxRating);
      return this;
    }

    public LastGamePrefsBuilder lastGameEnforceRating(boolean lastGameEnforceRating) {
      lastGamePrefs.setLastGameEnforceRating(lastGameEnforceRating);
      return this;
    }

    public LastGamePrefsBuilder lastGameOnlyFriends(boolean lastGameOnlyFriends) {
      lastGamePrefs.setLastGameOnlyFriends(lastGameOnlyFriends);
      return this;
    }
  }

  public class LocalizationPrefsBuilder extends SubPreferencesBuilder {
    private final LocalizationPrefs localizationPrefs = preferences.getLocalization();

    public LocalizationPrefsBuilder language(Locale language) {
      localizationPrefs.setLanguage(language);
      return this;
    }
  }

  public class LoginPrefsBuilder extends SubPreferencesBuilder {
    private final LoginPrefs loginPrefs = preferences.getLogin();

    public LoginPrefsBuilder refreshToken(String refreshToken) {
      loginPrefs.setRefreshToken(refreshToken);
      return this;
    }
  }

  public class NewsPrefsBuilder extends SubPreferencesBuilder {
    private final NewsPrefs newsPrefs = preferences.getNews();

    public NewsPrefsBuilder lastReadNewsUrl(String lastReadNewsUrl) {
      newsPrefs.setLastReadNewsUrl(lastReadNewsUrl);
      return this;
    }
  }

  public class NotificationsPrefsBuilder extends SubPreferencesBuilder {
    private final NotificationsPrefs notificationsPrefs = preferences.getNotification();

    public NotificationsPrefsBuilder soundsEnabled(boolean soundsEnabled) {
      notificationsPrefs.setSoundsEnabled(soundsEnabled);
      return this;
    }

    public NotificationsPrefsBuilder transientNotificationsEnabled(boolean transientNotificationsEnabled) {
      notificationsPrefs.setTransientNotificationsEnabled(transientNotificationsEnabled);
      return this;
    }

    public NotificationsPrefsBuilder mentionSoundEnabled(boolean mentionSoundEnabled) {
      notificationsPrefs.setMentionSoundEnabled(mentionSoundEnabled);
      return this;
    }

    public NotificationsPrefsBuilder infoSoundEnabled(boolean infoSoundEnabled) {
      notificationsPrefs.setInfoSoundEnabled(infoSoundEnabled);
      return this;
    }

    public NotificationsPrefsBuilder warnSoundEnabled(boolean warnSoundEnabled) {
      notificationsPrefs.setWarnSoundEnabled(warnSoundEnabled);
      return this;
    }

    public NotificationsPrefsBuilder errorSoundEnabled(boolean errorSoundEnabled) {
      notificationsPrefs.setErrorSoundEnabled(errorSoundEnabled);
      return this;
    }

    public NotificationsPrefsBuilder friendOnlineToastEnabled(boolean friendOnlineToastEnabled) {
      notificationsPrefs.setFriendOnlineToastEnabled(friendOnlineToastEnabled);
      return this;
    }

    public NotificationsPrefsBuilder friendOfflineToastEnabled(boolean friendOfflineToastEnabled) {
      notificationsPrefs.setFriendOfflineToastEnabled(friendOfflineToastEnabled);
      return this;
    }

    public NotificationsPrefsBuilder friendOnlineSoundEnabled(boolean friendOnlineSoundEnabled) {
      notificationsPrefs.setFriendOnlineSoundEnabled(friendOnlineSoundEnabled);
      return this;
    }

    public NotificationsPrefsBuilder friendOfflineSoundEnabled(boolean friendOfflineSoundEnabled) {
      notificationsPrefs.setFriendOfflineSoundEnabled(friendOfflineSoundEnabled);
      return this;
    }

    public NotificationsPrefsBuilder friendJoinsGameSoundEnabled(boolean friendJoinsGameSoundEnabled) {
      notificationsPrefs.setFriendJoinsGameSoundEnabled(friendJoinsGameSoundEnabled);
      return this;
    }

    public NotificationsPrefsBuilder friendPlaysGameSoundEnabled(boolean friendPlaysGameSoundEnabled) {
      notificationsPrefs.setFriendPlaysGameSoundEnabled(friendPlaysGameSoundEnabled);
      return this;
    }

    public NotificationsPrefsBuilder friendPlaysGameToastEnabled(boolean friendPlaysGameToastEnabled) {
      notificationsPrefs.setFriendPlaysGameToastEnabled(friendPlaysGameToastEnabled);
      return this;
    }

    public NotificationsPrefsBuilder privateMessageSoundEnabled(boolean privateMessageSoundEnabled) {
      notificationsPrefs.setPrivateMessageSoundEnabled(privateMessageSoundEnabled);
      return this;
    }

    public NotificationsPrefsBuilder privateMessageToastEnabled(boolean privateMessageToastEnabled) {
      notificationsPrefs.setPrivateMessageToastEnabled(privateMessageToastEnabled);
      return this;
    }

    public NotificationsPrefsBuilder friendJoinsGameToastEnabled(boolean friendJoinsGameToastEnabled) {
      notificationsPrefs.setFriendJoinsGameToastEnabled(friendJoinsGameToastEnabled);
      return this;
    }

    public NotificationsPrefsBuilder notifyOnAtMentionOnlyEnabled(boolean notifyOnAtMentionOnlyEnabled) {
      notificationsPrefs.setNotifyOnAtMentionOnlyEnabled(notifyOnAtMentionOnlyEnabled);
      return this;
    }

    public NotificationsPrefsBuilder afterGameReviewEnabled(boolean afterGameReviewEnabled) {
      notificationsPrefs.setAfterGameReviewEnabled(afterGameReviewEnabled);
      return this;
    }

    public NotificationsPrefsBuilder toastPosition(ToastPosition toastPosition) {
      notificationsPrefs.setToastPosition(toastPosition);
      return this;
    }

    public NotificationsPrefsBuilder toastScreen(int toastScreen) {
      notificationsPrefs.setToastScreen(toastScreen);
      return this;
    }

    public NotificationsPrefsBuilder toastDisplayTime(int toastDisplayTime) {
      notificationsPrefs.setToastDisplayTime(toastDisplayTime);
      return this;
    }
  }

  public class VaultPrefsBuilder extends SubPreferencesBuilder {
    private final VaultPrefs vaultPrefs = preferences.getVault();

    public VaultPrefsBuilder onlineReplaySortConfig(SortConfig onlineReplaySortConfig) {
      vaultPrefs.setOnlineReplaySortConfig(onlineReplaySortConfig);
      return this;
    }

    public VaultPrefsBuilder mapSortConfig(SortConfig mapSortConfig) {
      vaultPrefs.setMapSortConfig(mapSortConfig);
      return this;
    }

    public VaultPrefsBuilder modVaultConfig(SortConfig modVaultConfig) {
      vaultPrefs.setModVaultConfig(modVaultConfig);
      return this;
    }

    public VaultPrefsBuilder savedReplayQueries(ObservableMap<String, String> savedReplayQueries) {
      vaultPrefs.setSavedReplayQueries(savedReplayQueries);
      return this;
    }

    public VaultPrefsBuilder savedMapQueries(ObservableMap<String, String> savedMapQueries) {
      vaultPrefs.setSavedMapQueries(savedMapQueries);
      return this;
    }

    public VaultPrefsBuilder savedModQueries(ObservableMap<String, String> savedModQueries) {
      vaultPrefs.setSavedModQueries(savedModQueries);
      return this;
    }
  }

  public class WindowPrefsBuilder extends SubPreferencesBuilder {
    private final WindowPrefs windowPrefs = preferences.getMainWindow();

    public WindowPrefsBuilder width(int width) {
      windowPrefs.setWidth(width);
      return this;
    }

    public WindowPrefsBuilder height(int height) {
      windowPrefs.setHeight(height);
      return this;
    }

    public WindowPrefsBuilder maximized(boolean maximized) {
      windowPrefs.setMaximized(maximized);
      return this;
    }

    public WindowPrefsBuilder navigationItem(NavigationItem navigationItem) {
      windowPrefs.setNavigationItem(navigationItem);
      return this;
    }

    public WindowPrefsBuilder x(double x) {
      windowPrefs.setX(x);
      return this;
    }

    public WindowPrefsBuilder y(double y) {
      windowPrefs.setY(y);
      return this;
    }

    public WindowPrefsBuilder backgroundImagePath(Path backgroundImagePath) {
      windowPrefs.setBackgroundImagePath(backgroundImagePath);
      return this;
    }
  }
}
