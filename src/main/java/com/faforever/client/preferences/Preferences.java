package com.faforever.client.preferences;

public class Preferences {

  private WindowPrefs mainWindow;
  private ForgedAlliancePrefs supCom;
  private LoginPrefs login;
  private String theme;
  private String lastGameMod;
  private ChatPrefs chat;

  public Preferences() {
    login = new LoginPrefs();
    supCom = new ForgedAlliancePrefs();
    mainWindow = new WindowPrefs(800, 600);
    theme = "default";
    chat = new ChatPrefs();
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

  public ForgedAlliancePrefs getSupCom() {
    return supCom;
  }

  public String getLastGameMod() {
    return lastGameMod;
  }

  public ChatPrefs getChat() {
    return chat;
  }
}
