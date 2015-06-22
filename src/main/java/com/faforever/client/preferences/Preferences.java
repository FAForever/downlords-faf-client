package com.faforever.client.preferences;

import java.util.ArrayList;
import java.util.List;

public class Preferences {

  private WindowPrefs mainWindow;
  private ForgedAlliancePrefs forgedAlliance;
  private LoginPrefs login;
  private String theme;
  private String lastGameType;
  private ChatPrefs chat;
  private List<String> ignoredNotifications;
  private String lastGameTitle;
  private String lastMap;

  public Preferences() {
    login = new LoginPrefs();
    forgedAlliance = new ForgedAlliancePrefs();
    mainWindow = new WindowPrefs(800, 600);
    theme = "default";
    chat = new ChatPrefs();
    lastGameType = "faf";
    ignoredNotifications = new ArrayList<>();
  }

  public LoginPrefs getLogin() {
    return login;
  }

  public WindowPrefs getMainWindow() {
    return mainWindow;
  }

  public String getTheme() {
    return theme;
  }

  public ForgedAlliancePrefs getForgedAlliance() {
    return forgedAlliance;
  }

  public String getLastGameType() {
    return lastGameType;
  }

  public ChatPrefs getChat() {
    return chat;
  }

  public String getLastMap() {
    return lastMap;
  }

  public void setLastMap(String lastMap) {
    this.lastMap = lastMap;
  }

  // FIXME explain tatsu what the correct way of doing this is ;-)
  public List<String> lastMods;

  public List<String> getIgnoredNotifications() {
    return ignoredNotifications;
  }

  public void setLastGameTitle(String lastGameTitle) {
    this.lastGameTitle = lastGameTitle;
  }

  public String getLastGameTitle() {
    return lastGameTitle;
  }
}
