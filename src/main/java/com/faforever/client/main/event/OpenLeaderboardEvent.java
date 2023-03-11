package com.faforever.client.main.event;

import javafx.scene.control.Tab;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class OpenLeaderboardEvent extends NavigateEvent {
  Tab leagueTab;
  public OpenLeaderboardEvent(Tab leagueTab) {
    super(NavigationItem.LEADERBOARD);
    this.leagueTab = leagueTab;
  }
}
