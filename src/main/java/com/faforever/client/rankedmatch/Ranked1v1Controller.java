package com.faforever.client.rankedmatch;

import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Ranked1v1Controller extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final PseudoClass NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlighted-bar");

  private final Random random;

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
  public ScrollPane ranked1v1Root;
  public Label gamesPlayedLabel;
  public Label rankingLabel;
  public Label winLossRationLabel;
  public Label rankingOutOfLabel;

  @Inject
  GameService gameService;
  @Inject
  PreferencesService preferencesService;
  @Inject
  PlayerService playerService;
  @Inject
  Environment environment;
  @Inject
  LeaderboardService leaderboardService;
  @Inject
  I18n i18n;
  @Inject
  Locale locale;

  @VisibleForTesting
  HashMap<Faction, ToggleButton> factionsToButtons;

  private InvalidationListener playerRatingListener;
  private boolean initialized;

  public Ranked1v1Controller() {
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
    factionsToButtons.put(Faction.AEON, aeonButton);
    factionsToButtons.put(Faction.UEF, uefButton);
    factionsToButtons.put(Faction.CYBRAN, cybranButton);
    factionsToButtons.put(Faction.SERAPHIM, seraphimButton);

    setSearching(false);

    gameService.searching1v1Property().addListener((observable, oldValue, newValue) -> setSearching(newValue));

    ObservableList<Faction> factions = preferencesService.getPreferences().getRanked1v1().getFactions();
    for (Faction faction : factions) {
      factionsToButtons.get(faction).setSelected(true);
    }
    playButton.setDisable(factions.isEmpty());

    preferencesService.addUpdateListener(preferences -> {
      if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
        onCancelButtonClicked();
      }
    });
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
    gameService.stopSearchRanked1v1();
    setSearching(false);
  }

  public Node getRoot() {
    return ranked1v1Root;
  }

  public void onPlayButtonClicked() {
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      // FIXME implement user notification
      return;
    }

    setSearching(true);
    setFactionButtonsDisabled(true);

    ObservableList<Faction> factions = preferencesService.getPreferences().getRanked1v1().getFactions();

    Faction randomFaction = factions.get(random.nextInt(factions.size()));
    gameService.startSearchRanked1v1(randomFaction);
  }

  public void onFactionButtonClicked() {
    List<Faction> factions = factionsToButtons.entrySet().stream()
        .filter(entry -> entry.getValue().isSelected())
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    preferencesService.getPreferences().getRanked1v1().getFactions().setAll(factions);
    preferencesService.storeInBackground();

    playButton.setDisable(factions.isEmpty());
  }

  public void onDisplay() {
    if (initialized) {
      return;
    }

    playerService.currentPlayerProperty().addListener((observable, oldValue, newValue) -> {
      setCurrentPlayer(newValue);
    });
    setCurrentPlayer(playerService.getCurrentPlayer());


    initialized = true;
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
    int beta = environment.getProperty("rating.beta", int.class);
    float deviation = player.getLeaderboardRatingDeviation();

    if (deviation > beta) {
      int initialStandardDeviation = environment.getProperty("rating.initialStandardDeviation", int.class);
      ratingProgressIndicator.setProgress((initialStandardDeviation - deviation) / beta);
      ratingProgressIndicator.setVisible(true);
      ratingLabel.setVisible(false);
      ratingHintLabel.setText(i18n.get("ranked1v1.ratingProgress.stillLearning"));
    } else {
      ratingProgressIndicator.setVisible(false);
      ratingLabel.setVisible(true);
      ratingLabel.setText(String.format(locale, "%d", rating));

      updateRatingHint(rating);
    }

    leaderboardService.getRanked1v1Stats()
        .thenAccept(ranked1v1Stats -> {
          int totalPlayers = 0;
          for (Map.Entry<String, Integer> entry : ranked1v1Stats.getRatingDistribution().entrySet()) {
            totalPlayers += entry.getValue();
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
      logger.warn("Leaderboard entry could not be read for current player: " + currentPlayer.getUsername());
      return null;
    });
  }

  private void updateRatingHint(int rating) {
    if (rating < environment.getProperty("rating.low", int.class)) {
      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.low"));
    } else if (rating < environment.getProperty("rating.moderate", int.class)) {
      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.moderate"));
    } else if (rating < environment.getProperty("rating.good", int.class)) {
      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.good"));
    } else if (rating < environment.getProperty("rating.high", int.class)) {
      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.high"));
    } else if (rating < environment.getProperty("rating.top", int.class)) {
      ratingHintLabel.setText(i18n.get("ranked1v1.ratingHint.top"));
    }
  }

  @SuppressWarnings("unchecked")
  private void plotRatingDistributions(Ranked1v1Stats ranked1v1Stats, Player player) {
    XYChart.Series<String, Integer> series = new XYChart.Series<>();
    series.setName(i18n.get("ranked1v1.players"));
    series.getData().addAll(ranked1v1Stats.getRatingDistribution().entrySet().stream()
        .sorted((o1, o2) -> Integer.compare(parseInt(o1.getKey()), parseInt(o2.getKey())))
        .map(item -> {
          int rating = parseInt(item.getKey());
          XYChart.Data<String, Integer> data = new XYChart.Data<>(String.format(locale, "%d", rating), item.getValue());
          int currentPlayerRating = RatingUtil.getLeaderboardRating(player);
          if (rating == currentPlayerRating) {
            data.nodeProperty().addListener((observable, oldValue, newValue) -> {
              newValue.pseudoClassStateChanged(NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS, true);
            });
          }
          return data;
        })
        .collect(Collectors.toList()));

    Platform.runLater(() -> ratingDistributionChart.getData().setAll(series));
  }
}
