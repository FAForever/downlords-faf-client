package com.faforever.client.main.event;

import java.util.HashMap;

public enum NavigationItem {
  NEWS("theme/news.fxml"),
  CHAT("theme/chat/chat.fxml"),
  PLAY("theme/play/play.fxml"),
  VAULT("theme/vault/vault.fxml"),
  LEADERBOARD("theme/leaderboard/leaderboards.fxml"),
  UNITS("theme/units.fxml"),
  TUTORIALS("theme/tutorial.fxml"),
  TOURNAMENTS("theme/tournaments/tournaments.fxml");

  private static final HashMap<String, NavigationItem> fromString;

  static {
    fromString = new HashMap<>();
    for (NavigationItem item : values()) {
      fromString.put(item.name(), item);
    }
  }

  private final String fxmlFile;

  NavigationItem(String fxmlFile) {
    this.fxmlFile = fxmlFile;
  }

  public static NavigationItem fromString(String string) {
    if (string == null) {
      return NEWS;
    }
    return fromString.get(string);
  }

  public String getFxmlFile() {
    return fxmlFile;
  }
}
