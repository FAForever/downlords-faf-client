package com.faforever.client.chat;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementService;
import com.faforever.client.achievements.AchievementService.AchievementState;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.events.EventService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.OffsetDateTimeCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.Leaderboard;
import com.faforever.client.leaderboard.LeaderboardRating;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.NameRecord;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.PlayerAchievement;
import com.faforever.commons.api.dto.PlayerEvent;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.faforever.client.achievements.AchievementService.AchievementState.UNLOCKED;
import static com.faforever.client.events.EventService.EVENT_AEON_PLAYS;
import static com.faforever.client.events.EventService.EVENT_AEON_WINS;
import static com.faforever.client.events.EventService.EVENT_BUILT_AIR_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_LAND_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_NAVAL_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_TECH_1_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_TECH_2_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_TECH_3_UNITS;
import static com.faforever.client.events.EventService.EVENT_CYBRAN_PLAYS;
import static com.faforever.client.events.EventService.EVENT_CYBRAN_WINS;
import static com.faforever.client.events.EventService.EVENT_SERAPHIM_PLAYS;
import static com.faforever.client.events.EventService.EVENT_SERAPHIM_WINS;
import static com.faforever.client.events.EventService.EVENT_UEF_PLAYS;
import static com.faforever.client.events.EventService.EVENT_UEF_WINS;
import static javafx.collections.FXCollections.observableList;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@Slf4j
@RequiredArgsConstructor
public class UserInfoWindowController implements Controller<Node> {

  private final StatisticsService statisticsService;
  private final CountryFlagService countryFlagService;
  private final AchievementService achievementService;
  private final EventService eventService;
  private final I18n i18n;
  private final UiService uiService;
  private final TimeService timeService;
  private final PlayerService playerService;
  private final NotificationService notificationService;
  private final LeaderboardService leaderboardService;
  private final Map<String, AchievementItemController> achievementItemById = new HashMap<>();
  private final Map<String, AchievementDefinition> achievementDefinitionById = new HashMap<>();
  public Label lockedAchievementsHeaderLabel;
  public Label unlockedAchievementsHeaderLabel;
  public PieChart gamesPlayedChart;
  public PieChart techBuiltChart;
  public PieChart unitsBuiltChart;
  public StackedBarChart factionsChart;
  public Label gamesPlayedLabel;
  public HBox ratingsBox;
  public Label ratingsLabels;
  public Label ratingsValues;
  public Pane unlockedAchievementsHeader;
  public Pane lockedAchievementsHeader;
  public ScrollPane achievementsPane;
  public ImageView mostRecentAchievementImageView;
  public Label mostRecentAchievementDescriptionLabel;
  public Label loadingProgressLabel;
  public Pane mostRecentAchievementPane;
  public Label mostRecentAchievementNameLabel;
  public Pane lockedAchievementsContainer;
  public Pane unlockedAchievementsContainer;
  public NumberAxis yAxis;
  public NumberAxis xAxis;
  public PlayerRatingChart ratingHistoryChart;
  public VBox loadingHistoryPane;
  public ComboBox<TimePeriod> timePeriodComboBox;
  public ComboBox<Leaderboard> ratingTypeComboBox;
  public Label usernameLabel;
  public Label countryLabel;
  public ImageView countryImageView;
  public Pane userInfoRoot;
  public TableView<NameRecord> nameHistoryTable;
  public TableColumn<NameRecord, OffsetDateTime> changeDateColumn;
  public TableColumn<NameRecord, String> nameColumn;
  private Player player;
  private Window ownerWindow;
  private List<RatingHistoryDataPoint> ratingData;

  private static boolean isUnlocked(PlayerAchievement playerAchievement) {
    return UNLOCKED == AchievementState.valueOf(playerAchievement.getState().name());
  }

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(loadingHistoryPane, loadingProgressLabel, achievementsPane, mostRecentAchievementPane,
        unlockedAchievementsHeader, unlockedAchievementsContainer, lockedAchievementsHeader, lockedAchievementsContainer,
        ratingHistoryChart);

    unlockedAchievementsHeader.visibleProperty().bind(unlockedAchievementsContainer.visibleProperty());
    unlockedAchievementsContainer.visibleProperty().bind(Bindings.createBooleanBinding(
        () -> !unlockedAchievementsContainer.getChildren().isEmpty(), unlockedAchievementsContainer.getChildren()));

    lockedAchievementsHeader.visibleProperty().bind(lockedAchievementsContainer.visibleProperty());
    lockedAchievementsContainer.visibleProperty().bind(Bindings.createBooleanBinding(
        () -> !lockedAchievementsContainer.getChildren().isEmpty(), lockedAchievementsContainer.getChildren()));

    lockedAchievementsContainer.getChildren().addListener((InvalidationListener) observable ->
        lockedAchievementsHeaderLabel.setText(i18n.get("achievements.locked", lockedAchievementsContainer.getChildren().size()))
    );
    unlockedAchievementsContainer.getChildren().addListener((InvalidationListener) observable ->
        unlockedAchievementsHeaderLabel.setText(i18n.get("achievements.unlocked", unlockedAchievementsContainer.getChildren().size()))
    );

    nameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
    changeDateColumn.setCellValueFactory(param -> param.getValue().changeDateProperty());
    changeDateColumn.setCellFactory(param -> new OffsetDateTimeCell<>(timeService));

    timePeriodComboBox.setConverter(timePeriodStringConverter());

    timePeriodComboBox.getItems().addAll(TimePeriod.values());
    timePeriodComboBox.setValue(TimePeriod.ALL_TIME);

    leaderboardService.getLeaderboards().thenApply(leaderboards -> {
      ratingTypeComboBox.getItems().clear();
      ratingTypeComboBox.getItems().addAll(leaderboards);
      ratingTypeComboBox.setConverter(leaderboardStringConverter());
      ratingTypeComboBox.getSelectionModel().selectFirst();
      return null;
    });

    ratingData = Collections.emptyList();
    ratingHistoryChart.initializeTooltip(uiService);
  }

  public Region getRoot() {
    return userInfoRoot;
  }

  private void setAvailableAchievements(List<AchievementDefinition> achievementDefinitions) {
    ObservableList<Node> children = lockedAchievementsContainer.getChildren();
    JavaFxUtil.runLater(children::clear);

    achievementDefinitions.forEach(achievementDefinition -> {
      AchievementItemController controller = uiService.loadFxml("theme/achievement_item.fxml");
      controller.setAchievementDefinition(achievementDefinition);
      achievementDefinitionById.put(achievementDefinition.getId(), achievementDefinition);
      achievementItemById.put(achievementDefinition.getId(), controller);
      JavaFxUtil.runLater(() -> children.add(controller.getRoot()));
    });
  }

  public void setPlayer(Player player) {
    this.player = player;

    usernameLabel.setText(player.getUsername());
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresent(image -> countryImageView.setImage(image));
    gamesPlayedLabel.setText(i18n.number(player.getNumberOfGames()));

    updateNameHistory();
    updateRatingGrids();
    countryLabel.setText(i18n.getCountryNameLocalized(player.getCountry()));

    onRatingTypeChange();

    loadAchievements();
    eventService.getPlayerEvents(player.getId())
        .thenAccept(events -> {
          plotFactionsChart(events);
          plotUnitsByCategoriesChart(events);
          plotTechBuiltChart(events);
          plotGamesPlayedChart();
        })
        .exceptionally(throwable -> {
          log.warn("Could not load player events", throwable);
          notificationService.addImmediateErrorNotification(throwable, "userInfo.statistics.errorLoading");
          return null;
        });
  }

  private void updateRatingGrids() {
    leaderboardService.getLeaderboards().thenAccept(leaderboards -> {
      StringBuilder ratingNames = new StringBuilder();
      StringBuilder ratingNumbers = new StringBuilder();
      leaderboards.forEach(leaderboard -> {
        LeaderboardRating leaderboardRating = player.getLeaderboardRatings().get(leaderboard.getTechnicalName());
        if (leaderboardRating != null) {
          String leaderboardName = i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey());
          ratingNames.append(i18n.get("leaderboard.rating", leaderboardName)).append("\n");
          ratingNumbers.append(i18n.number(RatingUtil.getLeaderboardRating(player, leaderboard))).append("\n");
        }
      });
      JavaFxUtil.runLater(() -> {
        ratingsLabels.setText(ratingNames.toString());
        ratingsValues.setText(ratingNumbers.toString());
      });
    });
  }

  private void updateNameHistory() {
    playerService.getPlayersByIds(Collections.singletonList(player.getId()))
        .thenAccept(players -> nameHistoryTable.setItems(players.get(0).getNames()))
        .exceptionally(throwable -> {
          log.warn("Could not load player name history", throwable);
          notificationService.addImmediateErrorNotification(throwable, "userInfo.nameHistory.errorLoading");
          return null;
        });
  }

  private void loadAchievements() {
    enterAchievementsLoadingState();
    achievementService.getAchievementDefinitions()
        .exceptionally(throwable -> {
          log.warn("Player achievements could not be loaded", throwable);
          notificationService.addImmediateErrorNotification(throwable, "userInfo.achievements.errorLoading");
          return Collections.emptyList();
        })
        .thenAccept(this::setAvailableAchievements)
        .thenCompose(aVoid -> achievementService.getPlayerAchievements(player.getId()))
        .thenAccept(playerAchievements -> {
          updatePlayerAchievements(playerAchievements);
          enterAchievementsLoadedState();
        })
        .exceptionally(throwable -> {
          log.warn("Could not display achievement definitions", throwable);
          notificationService.addImmediateErrorNotification(throwable, "userInfo.achievements.errorLDisplaying");
          return null;
        });
  }

  private void plotFactionsChart(Map<String, PlayerEvent> playerEvents) {
    int aeonPlays = playerEvents.containsKey(EVENT_AEON_PLAYS) ? playerEvents.get(EVENT_AEON_PLAYS).getCurrentCount() : 0;
    int cybranPlays = playerEvents.containsKey(EVENT_CYBRAN_PLAYS) ? playerEvents.get(EVENT_CYBRAN_PLAYS).getCurrentCount() : 0;
    int uefPlays = playerEvents.containsKey(EVENT_UEF_PLAYS) ? playerEvents.get(EVENT_UEF_PLAYS).getCurrentCount() : 0;
    int seraphimPlays = playerEvents.containsKey(EVENT_SERAPHIM_PLAYS) ? playerEvents.get(EVENT_SERAPHIM_PLAYS).getCurrentCount() : 0;

    int aeonWins = playerEvents.containsKey(EVENT_AEON_WINS) ? playerEvents.get(EVENT_AEON_WINS).getCurrentCount() : 0;
    int cybranWins = playerEvents.containsKey(EVENT_CYBRAN_WINS) ? playerEvents.get(EVENT_CYBRAN_WINS).getCurrentCount() : 0;
    int uefWins = playerEvents.containsKey(EVENT_UEF_WINS) ? playerEvents.get(EVENT_UEF_WINS).getCurrentCount() : 0;
    int seraphimWins = playerEvents.containsKey(EVENT_SERAPHIM_WINS) ? playerEvents.get(EVENT_SERAPHIM_WINS).getCurrentCount() : 0;

    XYChart.Series<String, Integer> winsSeries = new XYChart.Series<>();
    winsSeries.setName(i18n.get("userInfo.wins"));
    winsSeries.getData().add(new XYChart.Data<>("Aeon", aeonWins));
    winsSeries.getData().add(new XYChart.Data<>("Cybran", cybranWins));
    winsSeries.getData().add(new XYChart.Data<>("UEF", uefWins));
    winsSeries.getData().add(new XYChart.Data<>("Seraphim", seraphimWins));

    XYChart.Series<String, Integer> lossSeries = new XYChart.Series<>();
    lossSeries.setName(i18n.get("userInfo.losses"));
    lossSeries.getData().add(new XYChart.Data<>("Aeon", aeonPlays - aeonWins));
    lossSeries.getData().add(new XYChart.Data<>("Cybran", cybranPlays - cybranWins));
    lossSeries.getData().add(new XYChart.Data<>("UEF", uefPlays - uefWins));
    lossSeries.getData().add(new XYChart.Data<>("Seraphim", seraphimPlays - seraphimWins));

    JavaFxUtil.runLater(() -> factionsChart.getData().addAll(winsSeries, lossSeries));
  }

  private void plotUnitsByCategoriesChart(Map<String, PlayerEvent> playerEvents) {
    int airBuilt = playerEvents.containsKey(EVENT_BUILT_AIR_UNITS) ? playerEvents.get(EVENT_BUILT_AIR_UNITS).getCurrentCount() : 0;
    int landBuilt = playerEvents.containsKey(EVENT_BUILT_LAND_UNITS) ? playerEvents.get(EVENT_BUILT_LAND_UNITS).getCurrentCount() : 0;
    int navalBuilt = playerEvents.containsKey(EVENT_BUILT_NAVAL_UNITS) ? playerEvents.get(EVENT_BUILT_NAVAL_UNITS).getCurrentCount() : 0;

    JavaFxUtil.runLater(() -> unitsBuiltChart.setData(FXCollections.observableArrayList(
        new PieChart.Data(i18n.get("stats.air"), airBuilt),
        new PieChart.Data(i18n.get("stats.land"), landBuilt),
        new PieChart.Data(i18n.get("stats.naval"), navalBuilt)
    )));
  }

  private void plotTechBuiltChart(Map<String, PlayerEvent> playerEvents) {
    int tech1Built = playerEvents.containsKey(EVENT_BUILT_TECH_1_UNITS) ? playerEvents.get(EVENT_BUILT_TECH_1_UNITS).getCurrentCount() : 0;
    int tech2Built = playerEvents.containsKey(EVENT_BUILT_TECH_2_UNITS) ? playerEvents.get(EVENT_BUILT_TECH_2_UNITS).getCurrentCount() : 0;
    int tech3Built = playerEvents.containsKey(EVENT_BUILT_TECH_3_UNITS) ? playerEvents.get(EVENT_BUILT_TECH_3_UNITS).getCurrentCount() : 0;

    JavaFxUtil.runLater(() -> techBuiltChart.setData(FXCollections.observableArrayList(
        new PieChart.Data(i18n.get("stats.tech1"), tech1Built),
        new PieChart.Data(i18n.get("stats.tech2"), tech2Built),
        new PieChart.Data(i18n.get("stats.tech3"), tech3Built)
    )));
  }

  private void plotGamesPlayedChart() {
    JavaFxUtil.runLater(() -> gamesPlayedChart.getData().clear());
    leaderboardService.getEntriesForPlayer(player.getId()).thenAccept(leaderboardEntries -> JavaFxUtil.runLater(() ->
        leaderboardEntries.forEach(leaderboardEntry ->
            gamesPlayedChart.getData().add(new PieChart.Data(i18n.getOrDefault(leaderboardEntry.getLeaderboard().getTechnicalName(), leaderboardEntry.getLeaderboard().getNameKey()),
                leaderboardEntry.getGamesPlayed())))))
        .exceptionally(throwable -> {
          log.warn("Leaderboard entry could not be read for player: " + player.getUsername(), throwable);
          return null;
        });
  }

  private void enterAchievementsLoadingState() {
    loadingProgressLabel.setVisible(true);
    achievementsPane.setVisible(false);
  }

  private void updatePlayerAchievements(List<? extends PlayerAchievement> playerAchievements) {
    PlayerAchievement mostRecentPlayerAchievement = null;

    ObservableList<Node> children = unlockedAchievementsContainer.getChildren();
    JavaFxUtil.runLater(children::clear);

    for (PlayerAchievement playerAchievement : playerAchievements) {
      AchievementItemController achievementItemController = achievementItemById.get(playerAchievement.getAchievement().getId());
      achievementItemController.setPlayerAchievement(playerAchievement);

      if (isUnlocked(playerAchievement)) {
        JavaFxUtil.runLater(() -> children.add(achievementItemController.getRoot()));
        if (mostRecentPlayerAchievement == null
            || playerAchievement.getUpdateTime().compareTo(mostRecentPlayerAchievement.getUpdateTime()) > 0) {
          mostRecentPlayerAchievement = playerAchievement;
        }
      }
    }

    if (mostRecentPlayerAchievement == null) {
      mostRecentAchievementPane.setVisible(false);
    } else {
      mostRecentAchievementPane.setVisible(true);
      AchievementDefinition mostRecentAchievement = achievementDefinitionById.get(mostRecentPlayerAchievement.getAchievement().getId());
      if (mostRecentAchievement == null) {
        return;
      }
      String mostRecentAchievementName = mostRecentAchievement.getName();
      String mostRecentAchievementDescription = mostRecentAchievement.getDescription();

      JavaFxUtil.runLater(() -> {
        mostRecentAchievementNameLabel.setText(mostRecentAchievementName);
        mostRecentAchievementDescriptionLabel.setText(mostRecentAchievementDescription);
        mostRecentAchievementImageView.setImage(achievementService.getImage(mostRecentAchievement, UNLOCKED));
      });
    }
  }

  private void enterAchievementsLoadedState() {
    loadingProgressLabel.setVisible(false);
    achievementsPane.setVisible(true);
  }

  public void onRatingTypeChange() {
    if (ratingTypeComboBox.getValue() != null) {
      ratingHistoryChart.setVisible(false);
      loadingHistoryPane.setVisible(true);
      loadStatistics(ratingTypeComboBox.getValue()).thenRun(() -> JavaFxUtil.runLater(this::plotPlayerRatingGraph));
    }
  }

  private CompletableFuture<Void> loadStatistics(Leaderboard leaderboard) {
    return statisticsService.getRatingHistory(player.getId(), leaderboard)
        .thenAccept(ratingHistory -> ratingData = ratingHistory)
        .exceptionally(throwable -> {
          // FIXME display to user
          log.warn("Statistics could not be loaded", throwable);
          return null;
        });
  }

  public void plotPlayerRatingGraph() {
    JavaFxUtil.assertApplicationThread();
    OffsetDateTime afterDate = OffsetDateTime.of(timePeriodComboBox.getValue().getDate(), ZoneOffset.UTC);
    List<XYChart.Data<Double, Double>> values = ratingData.stream().sorted(Comparator.comparing(RatingHistoryDataPoint::getInstant))
        .filter(dataPoint -> dataPoint.getInstant().isAfter(afterDate))
        .map(dataPoint -> new Data<>((double) dataPoint.getInstant().toEpochSecond(), (double) RatingUtil.getRating(dataPoint)))
        .collect(Collectors.toList());

    xAxis.setTickLabelFormatter(ratingLabelFormatter());
    if (values.size() > 0) {
      xAxis.setLowerBound(values.get(0).getXValue());
      xAxis.setUpperBound(values.get(values.size() - 1).getXValue());
    }
    xAxis.setTickUnit((xAxis.getUpperBound() - xAxis.getLowerBound()) / 10);

    XYChart.Series<Double, Double> series = new XYChart.Series<>(observableList(values));
    series.setName(i18n.get("userInfo.ratingOverTime"));
    ratingHistoryChart.setData(FXCollections.observableList(Collections.singletonList(series)));
    loadingHistoryPane.setVisible(false);
    ratingHistoryChart.setVisible(true);
  }

  @NotNull
  private StringConverter<Leaderboard> leaderboardStringConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(Leaderboard leaderboard) {
        return i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey());
      }

      @Override
      public Leaderboard fromString(String string) {
        return null;
      }
    };
  }

  @NotNull
  private StringConverter<TimePeriod> timePeriodStringConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(TimePeriod period) {
        return i18n.get(period.getI18nKey());
      }

      @Override
      public TimePeriod fromString(String string) {
        return null;
      }
    };
  }

  @NotNull
  private StringConverter<Number> ratingLabelFormatter() {
    return new StringConverter<>() {
      @Override
      public String toString(Number object) {
        long number = object.longValue();
        return timeService.asDate(Instant.ofEpochSecond(number));
      }

      @Override
      public Number fromString(String string) {
        return null;
      }
    };
  }

  public void show() {
    Assert.checkNullIllegalState(ownerWindow, "ownerWindow must be set");

    FxStage fxStage = FxStage.create(userInfoRoot)
        .initOwner(ownerWindow)
        .initModality(Modality.WINDOW_MODAL)
        .withSceneFactory(uiService::createScene)
        .allowMinimize(false)
        .apply();

    Stage stage = fxStage.getStage();
    stage.showingProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        userInfoRoot.getChildren().clear();
      }
    });
    stage.show();
  }

  public void setOwnerWindow(Window ownerWindow) {
    this.ownerWindow = ownerWindow;
  }

}
