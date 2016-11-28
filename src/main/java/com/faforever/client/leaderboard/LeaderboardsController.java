package com.faforever.client.leaderboard;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.game.KnownFeaturedMod;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LeaderboardsController extends AbstractViewController<Node> {
  public TabPane leaderboardRoot;
  public LeaderboardController ranked1v1LeaderboardController;
  public LeaderboardController rankedGlobalLeaderboardController;

  @Override
  public void initialize() {
    super.initialize();
    ranked1v1LeaderboardController.setRatingType(KnownFeaturedMod.LADDER_1V1);
    rankedGlobalLeaderboardController.setRatingType(KnownFeaturedMod.FAF);
  }

  @Override
  public Node getRoot() {
    return leaderboardRoot;
  }

}
