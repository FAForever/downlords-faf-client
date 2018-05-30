package com.faforever.client.vault.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.Controller;
import com.faforever.client.game.Game;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.util.TimeService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WatchButtonController implements Controller<Node> {
  private final ReplayService replayService;
  private final PlayerService playerService;
  private final ClientProperties clientProperties;
  private final TimeService timeService;

  public MenuButton watchButton;
  private Game game;
  private I18n i18n;
  private Timeline delayTimeline;

  public WatchButtonController(ReplayService replayService, PlayerService playerService, ClientProperties clientProperties, TimeService timeService, I18n i18n) {
    this.replayService = replayService;
    this.playerService = playerService;
    this.clientProperties = clientProperties;
    this.timeService = timeService;
    this.i18n = i18n;
  }

  public void initialize() {
    delayTimeline = new Timeline(
        new KeyFrame(Duration.ZERO, event -> updateWatchButtonTimer()),
        new KeyFrame(Duration.seconds(1))
    );
    delayTimeline.setCycleCount(Timeline.INDEFINITE);

    watchButton.setDisable(true);
  }

  public void setGame(Game game) {
    this.game = game;
    Assert.notNull(game.getStartTime(), "The game's start must not be null: " + game);

    List<MenuItem> menuItems = game.getTeams().values().stream()
        .flatMap(Collection::stream)
        .map(playerService::getPlayerForUsername)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(player -> createMenuItem(game, player))
        .collect(Collectors.toList());

    watchButton.getItems().setAll(menuItems);
    delayTimeline.play();
  }

  @NotNull
  private MenuItem createMenuItem(Game game, Player player) {
    MenuItem menuItem = new MenuItem(player.getUsername());
    menuItem.setUserData(player.getId());
    menuItem.setOnAction(event -> replayService.runLiveReplay(game.getId(), player.getId()));
    return menuItem;
  }

  private void updateWatchButtonTimer() {
    Assert.notNull(game, "Game must not be null");
    Assert.notNull(game.getStartTime(),
        "Game's start time is null, in which case it shouldn't even be listed: " + game);

    java.time.Duration watchDelay = java.time.Duration.between(
        Instant.now(),
        game.getStartTime().plusSeconds(clientProperties.getReplay().getWatchDelaySeconds())
    );
    if (watchDelay.isZero() || watchDelay.isNegative()) {
      delayTimeline.stop();
      delayTimeline = null;
      watchButton.setText(i18n.get("game.watch"));
      watchButton.setDisable(false);
    } else {
      watchButton.setText(i18n.get("game.watchDelayedFormat", timeService.shortDuration(watchDelay)));
    }
  }

  @Override
  public Node getRoot() {
    return watchButton;
  }
}
