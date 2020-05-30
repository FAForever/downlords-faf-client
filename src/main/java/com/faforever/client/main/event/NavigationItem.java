package com.faforever.client.main.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;

@RequiredArgsConstructor
@Getter
public enum NavigationItem {
  NEWS("theme/news.fxml", "main.news"),
  CHAT("theme/chat/chat.fxml", "main.chat"),
  PLAY("theme/play/play.fxml", "main.play"),
  VAULT("theme/vault/vault.fxml", "main.vault"),
  LEADERBOARD("theme/leaderboard/leaderboards.fxml", "main.leaderboards"),
  UNITS("theme/units.fxml", "main.units"),
  TUTORIALS("theme/tutorial.fxml", "main.tutorials"),
  TOURNAMENTS("theme/tournaments/tournaments.fxml", "main.tournaments");

  private static final HashMap<String, NavigationItem> fromString;

  static {
    fromString = new HashMap<>();
    for (NavigationItem item : values()) {
      fromString.put(item.name(), item);
    }
  }

  private final String fxmlFile;
  private final String i18nKey;

  public static NavigationItem fromString(String string) {
    if (string == null) {
      return NEWS;
    }
    return fromString.get(string);
  }

}
