package com.faforever.client.preferences;

import java.util.ArrayList;
import java.util.List;

public class Preferences {

  private WindowPrefs mainWindow;
  private ForgedAlliancePrefs forgedAlliance;
  private LoginPrefs login;
  private String theme;
  private String lastGameMod;
  private ChatPrefs chat;
  private String lastMap;
  private List<String> ignoredNotifications;

  public Preferences() {
    login = new LoginPrefs();
    forgedAlliance = new ForgedAlliancePrefs();
    mainWindow = new WindowPrefs(800, 600);
    theme = "default";
    chat = new ChatPrefs();
    lastGameMod = "faf";
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

  public String getLastGameMod() {
    return lastGameMod;
  }

  public ChatPrefs getChat() {
    return chat;
  }

  public String getLastMap() {
    return lastMap;
  }

  // FIXME explain tatsu what the correct way of doing this is ;-)
  public List<String> lastMods;

  public List<String> getIgnoredNotifications() {
    return ignoredNotifications;
  }
}
