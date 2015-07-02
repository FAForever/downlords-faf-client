package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import static javafx.collections.FXCollections.observableArrayList;

public class Preferences {

  private final WindowPrefs mainWindow;
  private final ForgedAlliancePrefs forgedAlliance;
  private final LoginPrefs login;
  private final ChatPrefs chat;
  private final NotificationsPrefs notification;

  private final StringProperty theme;
  private final StringProperty lastGameType;
  private final StringProperty lastGameTitle;
  private final StringProperty lastMap;
  private final BooleanProperty rememberLastTab;
  private final ListProperty<String> ignoredNotifications;

  public Preferences() {
    chat = new ChatPrefs();
    login = new LoginPrefs();
    mainWindow = new WindowPrefs();
    forgedAlliance = new ForgedAlliancePrefs();
    theme = new SimpleStringProperty("default");
    lastGameType = new SimpleStringProperty("faf");
    ignoredNotifications = new SimpleListProperty<>(observableArrayList());
    notification = new NotificationsPrefs();
    rememberLastTab = new SimpleBooleanProperty(false);
    lastGameTitle = new SimpleStringProperty();
    lastMap = new SimpleStringProperty();
  }

  public WindowPrefs getMainWindow() {
    return mainWindow;
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

  public String getTheme() {
    return theme.get();
  }

  public StringProperty themeProperty() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme.set(theme);
  }

  public String getLastGameType() {
    return lastGameType.get();
  }

  public StringProperty lastGameTypeProperty() {
    return lastGameType;
  }

  public void setLastGameType(String lastGameType) {
    this.lastGameType.set(lastGameType);
  }

  public String getLastGameTitle() {
    return lastGameTitle.get();
  }

  public StringProperty lastGameTitleProperty() {
    return lastGameTitle;
  }

  public void setLastGameTitle(String lastGameTitle) {
    this.lastGameTitle.set(lastGameTitle);
  }

  public String getLastMap() {
    return lastMap.get();
  }

  public StringProperty lastMapProperty() {
    return lastMap;
  }

  public void setLastMap(String lastMap) {
    this.lastMap.set(lastMap);
  }

  public boolean getRememberLastTab() {
    return rememberLastTab.get();
  }

  public BooleanProperty rememberLastTabProperty() {
    return rememberLastTab;
  }

  public void setRememberLastTab(boolean rememberLastTab) {
    this.rememberLastTab.set(rememberLastTab);
  }

  public ObservableList<String> getIgnoredNotifications() {
    return ignoredNotifications.get();
  }

  public ListProperty<String> ignoredNotificationsProperty() {
    return ignoredNotifications;
  }

  public void setIgnoredNotifications(ObservableList<String> ignoredNotifications) {
    this.ignoredNotifications.set(ignoredNotifications);
  }
}
