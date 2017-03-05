package com.faforever.client.rankedmatch;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.leaderboard.RatingStat;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.RatingUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static com.faforever.client.game.Faction.AEON;
import static com.faforever.client.game.Faction.CYBRAN;
import static com.faforever.client.game.Faction.SERAPHIM;
import static com.faforever.client.game.Faction.UEF;

@Component
@Lazy
public class Ladder1v1Controller extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final PseudoClass NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlighted-bar");

  private final Random random;
  private final GameService gameService;
  private final PreferencesService preferencesService;
  private final PlayerService playerService;
  private final LeaderboardService leaderboardService;
  private final I18n i18n;
  private final ClientProperties clientProperties;

  public CategoryAxis ratingDistributionXAxis;
  public NumberAxis ratingDistributionYAxis;
  public BarChart<String, Integer> ratingDistributionChart;
  public Label ratingHintLabel;
  public Label searchingForOpponentLabel;
  public Label ratingLabel;
  public ProgressIndicator searchProgressIndicator;
  public ProgressIndicator ratingProgressIndicator;
  public ToggleButton aeonButton;
  public ToggleButton uefButton;
  public ToggleButton cybranButton;
  public ToggleButton seraphimButton;
  public Button cancelButton;
  public Button playButton;
  public ScrollPane ladder1v1Root;
  public Label gamesPlayedLabel;
  public Label rankingLabel;
  public Label winLossRationLabel;
  public Label rankingOutOfLabel;
  @VisibleForTesting
  HashMap<Faction, ToggleButton> factionsToButtons;

  // Kept as a field in order to prevent garbage collection
  private InvalidationListener playerRatingListener;
  private boolean initialized;

  @Inject
  public Ladder1v1Controller(GameService gameService,
                             PreferencesService preferencesService,
                             PlayerService playerService,
                             LeaderboardService leaderboardService,
                             I18n i18n, ClientProperties clientProperties) {
    this.gameService = gameService;
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.leaderboardService = leaderboardService;
    this.i18n = i18n;
    this.clientProperties = clientProperties;

    random = new Random();
  }

  @Override
  public void initialize() {
    super.initialize();
    cancelButton.managedProperty().bind(cancelButton.visibleProperty());
    playButton.managedProperty().bind(playButton.visibleProperty());
    ratingLabel.managedProperty().bind(ratingLabel.visibleProperty());
    ratingProgressIndicator.managedProperty().bind(ratingProgressIndicator.visibleProperty());

    factionsToButtons = new HashMap<>();
    factionsToButtons.put(AEON, aeonButton);
    factionsToButtons.put(UEF, uefButton);
    factionsToButtons.put(CYBRAN, cybranButton);
    factionsToButtons.put(SERAPHIM, seraphimButton);

    setSearching(false);

    gameService.searching1v1Property().addListener((observable, oldValue, newValue) -> setSearching(newValue));

    ObservableList<Faction> factions = preferencesService.getPreferences().getLadder1v1Prefs().getFactions();
    for (Faction faction : EnumSet.of(AEON, CYBRAN, UEF, SERAPHIM)) {
      factionsToButtons.get(faction).setSelected(factions.contains(faction));
    }
    playButton.setDisable(factions.isEmpty());

    preferencesService.addUpdateListener(preferences -> {
      if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
        onCancelButtonClicked();
      }
    });

    playerService.currentPlayerProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> setCurrentPlayer(newValue)));
    playerService.getCurrentPlayer().ifPresent(this::setCurrentPlayer);
  }

  private void setSearching(boolean searching) {
    cancelButton.setVisible(searching);
    playButton.setVisible(!searching);
    searchProgressIndicator.setVisible(searching);
    searchingForOpponentLabel.setVisible(searching);
    setFactionButtonsDisabled(searching);
  }

  private void setFactionButtonsDisabled(boolean disabled) {
    factionsToButtons.values().forEach(button -> button.setDisable(disabled));
  }

  public void onCancelButtonClicked() {
    gameService.stopSearchLadder1v1();
    setSearching(false);
  }

  public Node getRoot() {
    return ladder1v1Root;
  }

  public void onPlayButtonClicked() {
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      // FIXME implement user notification
      return;
    }

    setSearching(true);
    setFactionButtonsDisabled(true);

    ObservableList<Faction> factions = preferencesService.getPreferences().getLadder1v1Prefs().getFactions();

    Faction randomFaction = factions.get(random.nextInt(factions.size()));
    gameService.startSearchLadder1v1(randomFaction);
  }

  public void onFactionButtonClicked() {
    List<Faction> factions = factionsToButtons.entrySet().stream()
        .filter(entry -> entry.getValue().isSelected())
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    preferencesService.getPreferences().getLadder1v1Prefs().getFactions().setAll(factions);
    preferencesService.storeInBackground();

    playButton.setDisable(factions.isEmpty());
  }

  private void setCurrentPlayer(Player player) {
    playerRatingListener = ratingObservable -> Platform.runLater(() -> updateRating(player));

    player.leaderboardRatingDeviationProperty().addListener(new WeakInvalidationListener(playerRatingListener));
    player.leaderboardRatingMeanProperty().addListener(new WeakInvalidationListener(playerRatingListener));
    updateRating(player);
    updateOtherValues(player);
  }

  private void updateRating(Player player) {
    int rating = RatingUtil.getLeaderboardRating(player);
    int beta = clientProperties.getTrueSkill().getBeta();
    float deviation = player.getLeaderboardRatingDeviation();

    if (deviation > beta) {
      int initialStandardDeviation = clientProperties.getTrueSkill().getInitialStandardDeviation();
      ratingProgressIndicator.setProgress((initialStandardDeviation - deviation) / beta);
      ratingProgressIndicator.setVisible(true);
      ratingLabel.setVisible(false);
      ratingHintLabel.setText(i18n.get("ranked1v1.ratingProgress.stillLearning"));
    } else {
      ratingProgressIndicator.setVisible(false);
      ratingLabel.setVisible(true);
      ratingLabel.setText(i18n.number(rating));

      updateRatingHint(rating);
    }

    leaderboardService.getLadder1v1Stats()
        .thenAccept(ranked1v1Stats -> {
          int totalPlayers = 0;
          for (RatingStat entry : ranked1v1Stats) {
            totalPlayers += entry.getCount();
          }
          plotRatingDistributions(ranked1v1Stats, player);
          String rankingOutOfText = i18n.get("ranked1v1.rankingOutOf", totalPlayers);
          Platform.runLater(() -> rankingOutOfLabel.setText(rankingOutOfText));
        })
        .exceptionally(throwable -> {
          logger.warn("Could not plot rating distribution", throwable);
          return null;
        });
  }

  private void updateOtherValues(Player currentPlayer) {
    leaderboardService.getEntryForPlayer(currentPlayer.getId()).thenAccept(leaderboardEntryBean -> Platform.runLater(() -> {
      rankingLabel.setText(i18n.get("ranked1v1.rankingFormat", leaderboardEntryBean.getRank()));
      gamesPlayedLabel.setText(String.format("%d", leaderboardEntryBean.getGamesPlayed()));
      winLossRationLabel.setText(i18n.get("percentage", leaderboardEntryBean.getWinLossRatio() * 100));
    })).exceptionally(throwable -> {
      logger.warn("Leaderboard entry could not be read for current player: " + currentPlayer.getUsername(), throwable);
      return null;
    });
  }

  private void updateRatingHint(int rating) {
    // TODO remove/rethink rating hint
//    if (rating < environment.getProperty("rating.low", int.class)) {
//      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.low"));
//    } else if (rating < environment.getProperty("rating.moderate", int.class)) {
//      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.moderate"));
//    } else if (rating < environment.getProperty("rating.good", int.class)) {
//      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.good"));
//    } else if (rating < environment.getProperty("rating.high", int.class)) {
//      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.high"));
//    } else if (rating < environment.getProperty("rating.top", int.class)) {
//      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.top"));
//    }
  }

  @SuppressWarnings("unchecked")
  private void plotRatingDistributions(List<RatingStat> ratingStats, Player player) {
    XYChart.Series<String, Integer> series = new XYChart.Series<>();
    series.setName(i18n.get("ranked1v1.players"));
    series.getData().addAll(ratingStats.stream()
        .sorted(Comparator.comparingInt(RatingStat::getCount))
        .map(item -> {
          int rating = item.getRating();
          XYChart.Data<String, Integer> data = new XYChart.Data<>(i18n.number(rating), item.getCount());
          int currentPlayerRating = RatingUtil.getLeaderboardRating(player);
          if (rating == (currentPlayerRating / 100) * 100) {
            data.nodeProperty().addListener((observable, oldValue, newValue)
                -> newValue.pseudoClassStateChanged(NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS, true));
          }
          return data;
        })
        .collect(Collectors.toList()));

    Platform.runLater(() -> ratingDistributionChart.getData().setAll(series));
  }
}
