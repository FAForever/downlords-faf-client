package com.faforever.client.preferences;

import com.faforever.client.game.KnownFeaturedMod;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import static javafx.collections.FXCollections.observableArrayList;

public class Preferences {

  public static final String DEFAULT_THEME_NAME = "default";

  private final WindowPrefs mainWindow;
  private final ForgedAlliancePrefs forgedAlliance;
  private final LoginPrefs login;
  private final ChatPrefs chat;
  private final NotificationsPrefs notification;
  private final StringProperty themeName;
  private final StringProperty lastGameType;
  private final LocalizationPrefs localization;
  private final StringProperty lastGameTitle;
  private final StringProperty lastMap;
  private final BooleanProperty rememberLastTab;
  private final ListProperty<String> ignoredNotifications;
  private final IntegerProperty lastGameMinRating;
  private final IntegerProperty lastGameMaxRating;
  private final StringProperty gamesViewMode;
  private final Ranked1v1Prefs ranked1v1;
  private final NewsPrefs news;
  private final DeveloperPrefs developer;

  public Preferences() {
    chat = new ChatPrefs();
    login = new LoginPrefs();
    localization = new LocalizationPrefs();
    mainWindow = new WindowPrefs();
    forgedAlliance = new ForgedAlliancePrefs();
    themeName = new SimpleStringProperty(DEFAULT_THEME_NAME);
    lastGameType = new SimpleStringProperty(KnownFeaturedMod.DEFAULT.getString());
    ignoredNotifications = new SimpleListProperty<>(observableArrayList());
    notification = new NotificationsPrefs();
    rememberLastTab = new SimpleBooleanProperty(true);
    lastGameTitle = new SimpleStringProperty();
    lastMap = new SimpleStringProperty();
    lastGameMinRating = new SimpleIntegerProperty(800);
    lastGameMaxRating = new SimpleIntegerProperty(1300);
    ranked1v1 = new Ranked1v1Prefs();
    gamesViewMode = new SimpleStringProperty();
    news = new NewsPrefs();
    developer = new DeveloperPrefs();
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

  public String getLastGameType() {
    return lastGameType.get();
  }

  public void setLastGameType(String lastGameType) {
    this.lastGameType.set(lastGameType);
  }

  public StringProperty lastGameTypeProperty() {
    return lastGameType;
  }

  public String getLastGameTitle() {
    return lastGameTitle.get();
  }

  public void setLastGameTitle(String lastGameTitle) {
    this.lastGameTitle.set(lastGameTitle);
  }

  public StringProperty lastGameTitleProperty() {
    return lastGameTitle;
  }

  public String getLastMap() {
    return lastMap.get();
  }

  public void setLastMap(String lastMap) {
    this.lastMap.set(lastMap);
  }

  public StringProperty lastMapProperty() {
    return lastMap;
  }

  public boolean getRememberLastTab() {
    return rememberLastTab.get();
  }

  public void setRememberLastTab(boolean rememberLastTab) {
    this.rememberLastTab.set(rememberLastTab);
  }

  public BooleanProperty rememberLastTabProperty() {
    return rememberLastTab;
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

  public int getLastGameMinRating() {
    return lastGameMinRating.get();
  }

  public void setLastGameMinRating(int lastGameMinRating) {
    this.lastGameMinRating.set(lastGameMinRating);
  }

  public IntegerProperty lastGameMinRatingProperty() {
    return lastGameMinRating;
  }

  public int getLastGameMaxRating() {
    return lastGameMaxRating.get();
  }

  public void setLastGameMaxRating(int lastGameMaxRating) {
    this.lastGameMaxRating.set(lastGameMaxRating);
  }

  public IntegerProperty lastGameMaxRatingProperty() {
    return lastGameMaxRating;
  }

  public Ranked1v1Prefs getRanked1v1() {
    return ranked1v1;
  }

  public NewsPrefs getNews() {
    return news;
  }

  public DeveloperPrefs getDeveloper() {
    return developer;
  }
}
