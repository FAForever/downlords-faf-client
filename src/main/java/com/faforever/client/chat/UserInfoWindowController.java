package com.faforever.client.chat;

import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementService;
import com.faforever.client.achievements.AchievementService.AchievementState;
import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.events.EventService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.OffsetDateTimeCell;
import com.faforever.client.fx.WindowController;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.NameRecord;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;
import static javafx.collections.FXCollections.observableList;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@Slf4j
public class UserInfoWindowController implements Controller<Node> {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM");

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
  public Label lockedAchievementsHeaderLabel;
  public Label unlockedAchievementsHeaderLabel;
  public PieChart gamesPlayedChart;
  public PieChart techBuiltChart;
  public PieChart unitsBuiltChart;
  public StackedBarChart factionsChart;
  public Label gamesPlayedLabel;
  public Label ratingLabelGlobal;
  public Label ratingLabel1v1;
  public ImageView avatarImageView;
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
  public ToggleButton globalButton;
  public ToggleButton ladder1v1Button;
  public NumberAxis yAxis;
  public NumberAxis xAxis;
  public LineChart<Integer, Integer> ratingHistoryChart;
  public Label usernameLabel;
  public Label countryLabel;
  public ImageView countryImageView;
  public Pane userInfoRoot;
  public TableView<NameRecord> nameHistoryTable;
  public TableColumn<NameRecord, OffsetDateTime> changeDateColumn;
  public TableColumn<NameRecord, String> nameColumn;
  private Player player;
  private Map<String, AchievementItemController> achievementItemById;
  private Map<String, AchievementDefinition> achievementDefinitionById;
  private Window ownerWindow;


  @Inject
  public UserInfoWindowController(StatisticsService statisticsService, CountryFlagService countryFlagService,
                                  AchievementService achievementService, EventService eventService, I18n i18n,
                                  UiService uiService, TimeService timeService,
                                  NotificationService notificationService, PlayerService playerService,
                                  LeaderboardService leaderboardService) {
    this.statisticsService = statisticsService;
    this.countryFlagService = countryFlagService;
    this.achievementService = achievementService;
    this.eventService = eventService;
    this.playerService = playerService;
    this.i18n = i18n;
    this.uiService = uiService;
    this.timeService = timeService;
    this.notificationService = notificationService;
    this.leaderboardService = leaderboardService;

    achievementItemById = new HashMap<>();
    achievementDefinitionById = new HashMap<>();
  }

  private static boolean isUnlocked(PlayerAchievement playerAchievement) {
    return UNLOCKED == AchievementState.valueOf(playerAchievement.getState().name());
  }

  public void initialize() {
    loadingProgressLabel.managedProperty().bind(loadingProgressLabel.visibleProperty());
    achievementsPane.managedProperty().bind(achievementsPane.visibleProperty());
    mostRecentAchievementPane.managedProperty().bind(mostRecentAchievementPane.visibleProperty());

    unlockedAchievementsHeader.managedProperty().bind(unlockedAchievementsHeader.visibleProperty());
    unlockedAchievementsHeader.visibleProperty().bind(unlockedAchievementsContainer.visibleProperty());
    unlockedAchievementsContainer.managedProperty().bind(unlockedAchievementsContainer.visibleProperty());
    unlockedAchievementsContainer.visibleProperty().bind(Bindings.createBooleanBinding(
        () -> !unlockedAchievementsContainer.getChildren().isEmpty(), unlockedAchievementsContainer.getChildren()));

    lockedAchievementsHeader.managedProperty().bind(lockedAchievementsHeader.visibleProperty());
    lockedAchievementsHeader.visibleProperty().bind(lockedAchievementsContainer.visibleProperty());
    lockedAchievementsContainer.managedProperty().bind(lockedAchievementsContainer.visibleProperty());
    lockedAchievementsContainer.visibleProperty().bind(Bindings.createBooleanBinding(
        () -> !lockedAchievementsContainer.getChildren().isEmpty(), lockedAchievementsContainer.getChildren()));

    lockedAchievementsContainer.getChildren().addListener((InvalidationListener) observable ->
        lockedAchievementsHeaderLabel.setText(i18n.get("achievements.locked", lockedAchievementsContainer.getChildren().size()))
    );
    unlockedAchievementsContainer.getChildren().addListener((InvalidationListener) observable ->
        unlockedAchievementsHeaderLabel.setText(i18n.get("achievements.unlocked", unlockedAchievementsContainer.getChildren().size()))
    );

    JavaFxUtil.addListener(getRoot().sceneProperty(), (observable, oldValue, newValue) -> {
      if (newValue != null) {
        JavaFxUtil.addListener(newValue.getWindow().showingProperty(), (observable11, oldValue11, newValue11) -> {
          if (!newValue11) {
            // Fixes #241
            userInfoRoot.getChildren().clear();
          }
        });
      }
    });

    nameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
    changeDateColumn.setCellValueFactory(param -> param.getValue().changeDateProperty());
    changeDateColumn.setCellFactory(param -> new OffsetDateTimeCell<>(timeService));
  }

  public Region getRoot() {
    return userInfoRoot;
  }

  private void setAvailableAchievements(List<AchievementDefinition> achievementDefinitions) {
    ObservableList<Node> children = lockedAchievementsContainer.getChildren();
    Platform.runLater(children::clear);

    achievementDefinitions.forEach(achievementDefinition -> {
      AchievementItemController controller = uiService.loadFxml("theme/achievement_item.fxml");
      controller.setAchievementDefinition(achievementDefinition);
      achievementDefinitionById.put(achievementDefinition.getId(), achievementDefinition);
      achievementItemById.put(achievementDefinition.getId(), controller);
      Platform.runLater(() -> children.add(controller.getRoot()));
    });
  }

  public void setPlayer(Player player) {
    this.player = player;

    usernameLabel.setText(player.getUsername());
    countryFlagService.loadCountryFlag(player.getCountry()).ifPresent(image -> countryImageView.setImage(image));
    avatarImageView.setImage(IdenticonUtil.createIdenticon(player.getId()));
    gamesPlayedLabel.setText(i18n.number(player.getNumberOfGames()));
    ratingLabelGlobal.setText(i18n.number(RatingUtil.getGlobalRating(player)));
    ratingLabel1v1.setText(i18n.number(RatingUtil.getLeaderboardRating(player)));

    updateNameHistory(player);

    countryLabel.setText(i18n.getCountryNameLocalized(player.getCountry()));

    globalButton.fire();
    globalButton.setSelected(true);

    loadAchievements();
    eventService.getPlayerEvents(player.getId())
        .thenAccept(events -> {
          plotFactionsChart(events);
          plotUnitsByCategoriesChart(events);
          plotTechBuiltChart(events);
          plotGamesPlayedChart();
        })
        .exceptionally(throwable -> {
          notificationService.addImmediateErrorNotification(throwable, "userInfo.statistics.errorLoading");
          log.warn("Could not load player events", throwable);
          return null;
        });
  }

  private void updateNameHistory(Player player) {
    playerService.getPlayersByIds(Collections.singletonList(player.getId()))
        .thenAccept(players -> nameHistoryTable.setItems(players.get(0).getNames()))
        .exceptionally(throwable -> {
          notificationService.addImmediateErrorNotification(throwable, "userInfo.nameHistory.errorLoading");
          return null;
        });
  }

  private void loadAchievements() {
    enterAchievementsLoadingState();
    achievementService.getAchievementDefinitions()
        .exceptionally(throwable -> {
          notificationService.addImmediateErrorNotification(throwable, "userInfo.achievements.errorLoading");
          log.warn("Player achievements could not be loaded", throwable);
          return Collections.emptyList();
        })
        .thenAccept(this::setAvailableAchievements)
        .thenCompose(aVoid -> achievementService.getPlayerAchievements(player.getId()))
        .thenAccept(playerAchievements -> {
          updatePlayerAchievements(playerAchievements);
          enterAchievementsLoadedState();
        })
        .exceptionally(throwable -> {
          notificationService.addImmediateErrorNotification(throwable, "userInfo.achievements.errorLDisplaying");
          log.warn("Could not display achievement definitions", throwable);

          return null;
        });
  }

  @SuppressWarnings("unchecked")
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

    Platform.runLater(() -> factionsChart.getData().addAll(winsSeries, lossSeries));
  }

  @SuppressWarnings("unchecked")
  private void plotUnitsByCategoriesChart(Map<String, PlayerEvent> playerEvents) {
    int airBuilt = playerEvents.containsKey(EVENT_BUILT_AIR_UNITS) ? playerEvents.get(EVENT_BUILT_AIR_UNITS).getCurrentCount() : 0;
    int landBuilt = playerEvents.containsKey(EVENT_BUILT_LAND_UNITS) ? playerEvents.get(EVENT_BUILT_LAND_UNITS).getCurrentCount() : 0;
    int navalBuilt = playerEvents.containsKey(EVENT_BUILT_NAVAL_UNITS) ? playerEvents.get(EVENT_BUILT_NAVAL_UNITS).getCurrentCount() : 0;

    Platform.runLater(() -> unitsBuiltChart.setData(FXCollections.observableArrayList(
        new PieChart.Data(i18n.get("stats.air"), airBuilt),
        new PieChart.Data(i18n.get("stats.land"), landBuilt),
        new PieChart.Data(i18n.get("stats.naval"), navalBuilt)
    )));
  }

  @SuppressWarnings("unchecked")
  private void plotTechBuiltChart(Map<String, PlayerEvent> playerEvents) {
    int tech1Built = playerEvents.containsKey(EVENT_BUILT_TECH_1_UNITS) ? playerEvents.get(EVENT_BUILT_TECH_1_UNITS).getCurrentCount() : 0;
    int tech2Built = playerEvents.containsKey(EVENT_BUILT_TECH_2_UNITS) ? playerEvents.get(EVENT_BUILT_TECH_2_UNITS).getCurrentCount() : 0;
    int tech3Built = playerEvents.containsKey(EVENT_BUILT_TECH_3_UNITS) ? playerEvents.get(EVENT_BUILT_TECH_3_UNITS).getCurrentCount() : 0;

    Platform.runLater(() -> techBuiltChart.setData(FXCollections.observableArrayList(
        new PieChart.Data(i18n.get("stats.tech1"), tech1Built),
        new PieChart.Data(i18n.get("stats.tech2"), tech2Built),
        new PieChart.Data(i18n.get("stats.tech3"), tech3Built)
    )));
  }

  @SuppressWarnings("unchecked")
  private void plotGamesPlayedChart() {
    Player currentPlayer = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player must be set"));
    leaderboardService.getEntryForPlayer(currentPlayer.getId()).thenAccept(leaderboardEntryBean -> Platform.runLater(() -> {
      int ladderGamesCount = leaderboardEntryBean.getGamesPlayed();
      int custonGamesCount = currentPlayer.getNumberOfGames();
      Platform.runLater(() -> gamesPlayedChart.setData(FXCollections.observableArrayList(
          new PieChart.Data(i18n.get("stats.custom"), custonGamesCount),
          new PieChart.Data(i18n.get("stats.ranked1v1"), ladderGamesCount)
      )));
    })).exceptionally(throwable -> {
      log.warn("Leaderboard entry could not be read for current player: " + currentPlayer.getUsername(), throwable);
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
    Platform.runLater(children::clear);

    for (PlayerAchievement playerAchievement : playerAchievements) {
      AchievementItemController achievementItemController = achievementItemById.get(playerAchievement.getAchievement().getId());
      achievementItemController.setPlayerAchievement(playerAchievement);

      if (isUnlocked(playerAchievement)) {
        Platform.runLater(() -> children.add(achievementItemController.getRoot()));
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

      Platform.runLater(() -> {
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

  public void ladder1v1ButtonClicked() {
    loadStatistics(KnownFeaturedMod.LADDER_1V1);
  }

  private CompletableFuture<Void> loadStatistics(KnownFeaturedMod featuredMod) {
    return statisticsService.getRatingHistory(featuredMod, player.getId())
        .thenAccept(ratingHistory -> Platform.runLater(() -> plotPlayerRatingGraph(ratingHistory)))
        .exceptionally(throwable -> {
          // FIXME display to user
          log.warn("Statistics could not be loaded", throwable);
          return null;
        });
  }

  @SuppressWarnings("unchecked")
  private void plotPlayerRatingGraph(List<RatingHistoryDataPoint> dataPoints) {
    List<XYChart.Data<Integer, Integer>> values = dataPoints.stream()
        .map(datapoint -> new Data<>(dataPoints.indexOf(datapoint), RatingUtil.getRating(datapoint)))
        .collect(Collectors.toList());

    xAxis.setTickLabelFormatter(ratingLabelFormatter(dataPoints));

    XYChart.Series<Integer, Integer> series = new XYChart.Series<>(observableList(values));
    series.setName(i18n.get("userInfo.ratingOverTime"));
    ratingHistoryChart.getData().setAll(series);
  }

  @NotNull
  private StringConverter<Number> ratingLabelFormatter(final List<RatingHistoryDataPoint> dataPoints) {
    return new StringConverter<Number>() {
      @Override
      public String toString(Number object) {
        int number = object.intValue();
        int numberOfDataPoints = dataPoints.size();
        int dataPointIndex = number >= numberOfDataPoints ? numberOfDataPoints - 1 : number;
        if (dataPointIndex >= dataPoints.size() || dataPointIndex < 0) {
          return "";
        }
        return DATE_FORMATTER.format(dataPoints.get(dataPointIndex).getInstant());
      }

      @Override
      public Number fromString(String string) {
        return null;
      }
    };
  }

  public void globalButtonClicked() {
    loadStatistics(KnownFeaturedMod.FAF);
  }

  public void show() {
    Assert.checkNullIllegalState(ownerWindow, "ownerWindow must be set");
    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(ownerWindow);

    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(userInfoWindow, userInfoRoot, true, CLOSE);

    userInfoWindow.show();
  }

  public void setOwnerWindow(Window ownerWindow) {
    this.ownerWindow = ownerWindow;
  }

}
