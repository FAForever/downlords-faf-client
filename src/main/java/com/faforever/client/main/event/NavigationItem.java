package com.faforever.client.main.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum NavigationItem {
  NEWS("theme/news.fxml", "main.news"),
  CHAT("theme/chat/chat.fxml", "main.chat"),
  PLAY("theme/play/play.fxml", "main.play"),
  REPLAY("theme/vault/replay.fxml", "main.replay"),
  MAP("theme/vault/map.fxml", "main.maps"),
  MOD("theme/vault/mod.fxml", "main.mods"),
  LEADERBOARD("theme/leaderboard/leaderboards.fxml", "main.leaderboards"),
  UNITS("theme/units.fxml", "main.units"),
  TUTORIALS("theme/tutorial.fxml", "main.tutorials"),
  TOURNAMENTS("theme/tournaments/tournaments.fxml", "main.tournaments");

  private final String fxmlFile;
  private final String i18nKey;
}
