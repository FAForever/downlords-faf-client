package com.faforever.client.rankedmatch;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.leaderboard.RatingDistribution;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.RatingUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Ranked1v1Controller {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final PseudoClass NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlighted-bar");

  private final Random random;

  @FXML
  CategoryAxis ratingDistributionXAxis;
  @FXML
  NumberAxis ratingDistributionYAxis;
  @FXML
  BarChart<String, Integer> ratingDistributionChart;
  @FXML
  Label ratingHintLabel;
  @FXML
  Label searchingForOpponentLabel;
  @FXML
  Label ratingLabel;
  @FXML
  ProgressIndicator searchProgressIndicator;
  @FXML
  ProgressIndicator ratingProgressIndicator;
  @FXML
  ToggleButton aeonButton;
  @FXML
  ToggleButton uefButton;
  @FXML
  ToggleButton cybranButton;
  @FXML
  ToggleButton seraphimButton;
  @FXML
  Button cancelButton;
  @FXML
  Button playButton;
  @FXML
  Pane ranked1v1Root;

  @Autowired
  GameService gameService;
  @Autowired
  PreferencesService preferencesService;
  @Autowired
  PlayerService playerService;
  @Autowired
  Environment environment;
  @Autowired
  LeaderboardService leaderboardService;
  @Autowired
  I18n i18n;

  @VisibleForTesting
  HashMap<Faction, ToggleButton> factionsToButtons;

  private InvalidationListener playerRatingListener;
  private boolean initialized;

  public Ranked1v1Controller() {
    random = new Random();
  }

  @FXML
  void initialize() {
    cancelButton.managedProperty().bind(cancelButton.visibleProperty());
    playButton.managedProperty().bind(playButton.visibleProperty());
    ratingLabel.managedProperty().bind(ratingLabel.visibleProperty());
    ratingProgressIndicator.managedProperty().bind(ratingProgressIndicator.visibleProperty());

    cancelButton.setVisible(false);
    playButton.setVisible(true);
    searchProgressIndicator.setVisible(false);
    searchingForOpponentLabel.setVisible(false);
    ratingLabel.setVisible(false);

    factionsToButtons = new HashMap<>();
    factionsToButtons.put(Faction.AEON, aeonButton);
    factionsToButtons.put(Faction.UEF, uefButton);
    factionsToButtons.put(Faction.CYBRAN, cybranButton);
    factionsToButtons.put(Faction.SERAPHIM, seraphimButton);
  }

  @PostConstruct
  void postConstruct() {
    gameService.searching1v1Property().addListener((observable, oldValue, newValue) -> {
      setSearching(newValue);
    });

    ObservableList<Faction> factions = preferencesService.getPreferences().getRanked1v1().getFactions();
    for (Faction faction : factions) {
      factionsToButtons.get(faction).setSelected(true);
    }

    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      playButton.setDisable(true);
      playButton.setTooltip(new Tooltip(i18n.get("missingGamePath.notification")));
    }

    preferencesService.addUpdateListener(preferences -> {
      if (preferencesService.getPreferences().getForgedAlliance().getPath() != null) {
        playButton.setDisable(false);
        playButton.setTooltip(null);
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

  public Node getRoot() {
    return ranked1v1Root;
  }

  @FXML
  void onCancelButtonClicked() {
    gameService.stopSearchRanked1v1();
    setSearching(false);
  }

  @FXML
  void onPlayButtonClicked() {
    cancelButton.setVisible(true);
    playButton.setVisible(false);
    searchProgressIndicator.setVisible(true);
    searchingForOpponentLabel.setVisible(true);
    setFactionButtonsDisabled(true);

    ObservableList<Faction> factions = preferencesService.getPreferences().getRanked1v1().getFactions();

    Faction randomFaction = factions.get(random.nextInt(factions.size()));
    gameService.startSearchRanked1v1(randomFaction);
  }

  @FXML
  void onFactionButtonClicked() {
    List<Faction> factions = factionsToButtons.entrySet().stream()
        .filter(entry -> entry.getValue().isSelected())
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    preferencesService.getPreferences().getRanked1v1().getFactions().setAll(factions);
    preferencesService.storeInBackground();

    playButton.setDisable(factions.isEmpty());
  }

  public void setUpIfNecessary() {
    if (initialized) {
      return;
    }

    initialized = true;
    PlayerInfoBean currentPlayer = playerService.getCurrentPlayer();
    playerRatingListener = observable -> Platform.runLater(() -> updateRating(currentPlayer));

    currentPlayer.leaderboardRatingDeviationProperty().addListener(playerRatingListener);
    currentPlayer.leaderboardRatingMeanProperty().addListener(playerRatingListener);
    updateRating(currentPlayer);
  }

  private void updateRating(PlayerInfoBean player) {
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
      ratingLabel.setText(String.valueOf(rating));

      updateRatingHint(rating);
    }

    leaderboardService.getRatingDistributions()
        .thenAccept(ratingDistributions -> plotRatingDistributions(ratingDistributions, player))
        .exceptionally(throwable -> {
          logger.warn("Could not plot rating distribution", throwable);
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
  private void plotRatingDistributions(List<RatingDistribution> ratingDistributionMap, PlayerInfoBean player) {
    XYChart.Series<String, Integer> series = new XYChart.Series<>();
    // FIXME i18n
    series.setName("ranked1v1");
    series.getData().addAll(ratingDistributionMap.stream()
        .map(item -> {
          int maxRating = item.getMaxRating();
          XYChart.Data<String, Integer> data = new XYChart.Data<>(String.valueOf(maxRating), item.getPlayers());
          if (maxRating == RatingUtil.getRoundedLeaderboardRating(player)) {
            data.setExtraValue("You");
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
