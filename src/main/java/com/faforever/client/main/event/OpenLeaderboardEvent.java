package com.faforever.client.main.event;

import javafx.scene.control.Tab;
import lombok.Getter;
import lombok.Setter;

public class OpenLeaderboardEvent extends NavigateEvent {
  @Getter
  @Setter
  private Tab leagueTab;
  public OpenLeaderboardEvent(Tab leagueTab) {
    super(NavigationItem.LEADERBOARD);
    this.leagueTab = leagueTab;
  }
}
