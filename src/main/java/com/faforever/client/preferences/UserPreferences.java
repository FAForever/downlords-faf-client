package com.faforever.client.preferences;

public class UserPreferences {

  private WindowPrefs mainWindow;
  private LoginPrefs login;

  public UserPreferences() {
    login = new LoginPrefs();
    mainWindow = new WindowPrefs(800, 600);
  }

  public LoginPrefs getLoginPrefs() {
    return login;
  }

  public WindowPrefs getMainWindow() {
    return mainWindow;
  }
}
