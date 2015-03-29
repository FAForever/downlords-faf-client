package com.faforever.client.preferences;

public class Preferences {

  private WindowPrefs mainWindow;
  private LoginPrefs login;
  private String theme;

  public Preferences() {
    login = new LoginPrefs();
    mainWindow = new WindowPrefs(800, 600);
    theme = "default";
  }

  public LoginPrefs getLoginPrefs() {
    return login;
  }

  public WindowPrefs getMainWindow() {
    return mainWindow;
  }

  public String getTheme() {
    return theme;
  }
}
