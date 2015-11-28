package com.faforever.client.chat;

import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementService;
import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.api.PlayerEvent;
import com.faforever.client.events.EventService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.stats.PlayerStatisticsMessageLobby;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.neovisionaries.i18n.CountryCode;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.api.AchievementState.UNLOCKED;
import static com.faforever.client.events.EventService.EVENT_AEON_PLAYS;
import static com.faforever.client.events.EventService.EVENT_AEON_WINS;
import static com.faforever.client.events.EventService.EVENT_BUILT_AIR_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_LAND_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_NAVAL_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_TECH_1_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_TECH_2_UNITS;
import static com.faforever.client.events.EventService.EVENT_BUILT_TECH_3_UNITS;
import static com.faforever.client.events.EventService.EVENT_CUSTOM_GAMES_PLAYED;
import static com.faforever.client.events.EventService.EVENT_CYBRAN_PLAYS;
import static com.faforever.client.events.EventService.EVENT_CYBRAN_WINS;
import static com.faforever.client.events.EventService.EVENT_RANKED_1V1_GAMES_PLAYED;
import static com.faforever.client.events.EventService.EVENT_SERAPHIM_PLAYS;
import static com.faforever.client.events.EventService.EVENT_SERAPHIM_WINS;
import static com.faforever.client.events.EventService.EVENT_UEF_PLAYS;
import static com.faforever.client.events.EventService.EVENT_UEF_WINS;

public class UserInfoWindowController {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM");
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  PieChart gamesPlayedChart;
  @FXML
  PieChart techBuiltChart;
  @FXML
  PieChart unitsBuiltChart;
  @FXML
  CategoryAxis factionsCategoryAxis;
  @FXML
  NumberAxis factionsNumberAxis;
  @FXML
  StackedBarChart factionsChart;
  @FXML
  Label gamesPlayedLabel;
  @FXML
  Label ratingLabelGlobal;
  @FXML
  Label ratingLabel1v1;
  @FXML
  ImageView avatarImageView;
  @FXML
  Pane unlockedAchievementsHeader;
  @FXML
  Pane lockedAchievementsHeader;
  @FXML
  ScrollPane achievementsPane;
  @FXML
  Label unlockedOfTotalLabel;
  @FXML
  Label pointsOfTotalLabel;
  @FXML
  ImageView mostRecentAchievementImageView;
  @FXML
  Label mostRecentAchievementDescriptionLabel;
  @FXML
  Label loadingProgressLabel;
  @FXML
  Pane mostRecentAchievementPane;
  @FXML
  Label mostRecentAchievementNameLabel;
  @FXML
  Pane lockedAchievementsContainer;
  @FXML
  Pane unlockedAchievementsContainer;
  @FXML
  ToggleButton ratingOver365DaysButton;
  @FXML
  ToggleButton ratingOver90DaysButton;
  @FXML
  NumberAxis rating90DaysYAxis;
  @FXML
  NumberAxis rating90DaysXAxis;
  @FXML
  LineChart<Long, Integer> rating90DaysChart;
  @FXML
  Label usernameLabel;
  @FXML
  Label countryLabel;
  @FXML
  ImageView countryImageView;
  @FXML
  Region userInfoRoot;

  @Resource
  StatisticsService statisticsService;
  @Resource
  CountryFlagService countryFlagService;
  @Resource
  AchievementService achievementService;
  @Resource
  EventService eventService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  I18n i18n;
  @Resource
  Locale locale;

  private PlayerInfoBean playerInfoBean;
  private Map<String, AchievementItemController> achievementItemById;
  private Map<String, AchievementDefinition> achievementDefinitionById;
  private int earnedExperiencePoints;

  public UserInfoWindowController() {
    achievementItemById = new HashMap<>();
    achievementDefinitionById = new HashMap<>();
  }

  @FXML
  void initialize() {
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

    rating90DaysXAxis.setTickLabelFormatter(new StringConverter<Number>() {
      @Override
      public String toString(Number object) {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(object.longValue()), ZoneId.systemDefault());
        return DATE_FORMATTER.format(zonedDateTime);
      }

      @Override
      public Number fromString(String string) {
        return null;
      }
    });
  }

  private void displayAvailableAchievements(List<AchievementDefinition> achievementDefinitions) {
    ObservableList<Node> children = lockedAchievementsContainer.getChildren();
    Platform.runLater(children::clear);

    achievementDefinitions.forEach(achievementDefinition -> {
      AchievementItemController controller = applicationContext.getBean(AchievementItemController.class);
      controller.setAchievementDefinition(achievementDefinition);
      achievementDefinitionById.put(achievementDefinition.getId(), achievementDefinition);
      achievementItemById.put(achievementDefinition.getId(), controller);
      Platform.runLater(() -> children.add(controller.getRoot()));
    });
  }

  public void setPlayerInfoBean(PlayerInfoBean playerInfoBean) {
    this.playerInfoBean = playerInfoBean;

    usernameLabel.textProperty().bind(playerInfoBean.usernameProperty());
    countryImageView.setImage(countryFlagService.loadCountryFlag(playerInfoBean.getCountry()));
    avatarImageView.setImage(IdenticonUtil.createIdenticon(playerInfoBean.getId()));
    gamesPlayedLabel.setText(String.format(locale, "%d", playerInfoBean.getNumberOfGames()));
    ratingLabelGlobal.setText(String.format(locale, "%d", RatingUtil.getGlobalRating(playerInfoBean)));
    ratingLabel1v1.setText(String.format(locale, "%d", RatingUtil.getGlobalRating(playerInfoBean)));

    CountryCode countryCode = CountryCode.getByCode(playerInfoBean.getCountry());
    if (countryCode != null) {
      // Country code is unknown to CountryCode, like A1 or A2 (from GeoIP)
      countryLabel.setText(countryCode.getName());
    } else {
      countryLabel.setText(playerInfoBean.getCountry());
    }

    ratingOver90DaysButton.setSelected(true);
    ratingOver90DaysButton.fire();

    loadAchievements();
    eventService.getPlayerEvents(playerInfoBean.getUsername()).thenAccept(events -> {
      plotFactionsChart(events);
      plotUnitsByCategoriesChart(events);
      plotTechBuiltChart(events);
      plotGamesPlayedChart(events);
    });
  }

  private void loadAchievements() {
    enterAchievementsLoadingState();
    achievementService.getAchievementDefinitions()
        .exceptionally(throwable -> {
          logger.warn("Could not load achievement definitions", throwable);
          return null;
        })
        .thenAccept(this::displayAvailableAchievements)
        .thenRun(() -> {
          ObservableList<PlayerAchievement> playerAchievements = achievementService.getPlayerAchievements(playerInfoBean.getUsername());
          playerAchievements.addListener((Observable observable) -> {
            updatePlayerAchievements(playerAchievements);
          });
          updatePlayerAchievements(playerAchievements);
          enterAchievementsLoadedState();
        })
        .exceptionally(throwable -> {
          // TODO tell the user
          logger.warn("Player achievements could not be loaded", throwable);
          return null;
        });
  }

  @SuppressWarnings("unchecked")
  private void plotFactionsChart(Map<String, PlayerEvent> playerEvents) {
    int aeonPlays = playerEvents.containsKey(EVENT_AEON_PLAYS) ? playerEvents.get(EVENT_AEON_PLAYS).getCount() : 0;
    int cybranPlays = playerEvents.containsKey(EVENT_CYBRAN_PLAYS) ? playerEvents.get(EVENT_CYBRAN_PLAYS).getCount() : 0;
    int uefPlays = playerEvents.containsKey(EVENT_UEF_PLAYS) ? playerEvents.get(EVENT_UEF_PLAYS).getCount() : 0;
    int seraphimPlays = playerEvents.containsKey(EVENT_SERAPHIM_PLAYS) ? playerEvents.get(EVENT_SERAPHIM_PLAYS).getCount() : 0;

    int aeonWins = playerEvents.containsKey(EVENT_AEON_WINS) ? playerEvents.get(EVENT_AEON_WINS).getCount() : 0;
    int cybranWins = playerEvents.containsKey(EVENT_CYBRAN_WINS) ? playerEvents.get(EVENT_CYBRAN_WINS).getCount() : 0;
    int uefWins = playerEvents.containsKey(EVENT_UEF_WINS) ? playerEvents.get(EVENT_UEF_WINS).getCount() : 0;
    int seraphimWins = playerEvents.containsKey(EVENT_SERAPHIM_WINS) ? playerEvents.get(EVENT_SERAPHIM_WINS).getCount() : 0;

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
    int airBuilt = playerEvents.containsKey(EVENT_BUILT_AIR_UNITS) ? playerEvents.get(EVENT_BUILT_AIR_UNITS).getCount() : 0;
    int landBuilt = playerEvents.containsKey(EVENT_BUILT_LAND_UNITS) ? playerEvents.get(EVENT_BUILT_LAND_UNITS).getCount() : 0;
    int navalBuilt = playerEvents.containsKey(EVENT_BUILT_NAVAL_UNITS) ? playerEvents.get(EVENT_BUILT_NAVAL_UNITS).getCount() : 0;

    Platform.runLater(() -> unitsBuiltChart.setData(FXCollections.observableArrayList(
        new PieChart.Data(i18n.get("stats.air"), airBuilt),
        new PieChart.Data(i18n.get("stats.land"), landBuilt),
        new PieChart.Data(i18n.get("stats.naval"), navalBuilt)
    )));
  }

  @SuppressWarnings("unchecked")
  private void plotTechBuiltChart(Map<String, PlayerEvent> playerEvents) {
    int tech1Built = playerEvents.containsKey(EVENT_BUILT_TECH_1_UNITS) ? playerEvents.get(EVENT_BUILT_TECH_1_UNITS).getCount() : 0;
    int tech2Built = playerEvents.containsKey(EVENT_BUILT_TECH_2_UNITS) ? playerEvents.get(EVENT_BUILT_TECH_2_UNITS).getCount() : 0;
    int tech3Built = playerEvents.containsKey(EVENT_BUILT_TECH_3_UNITS) ? playerEvents.get(EVENT_BUILT_TECH_3_UNITS).getCount() : 0;

    Platform.runLater(() -> techBuiltChart.setData(FXCollections.observableArrayList(
        new PieChart.Data(i18n.get("stats.tech1"), tech1Built),
        new PieChart.Data(i18n.get("stats.tech2"), tech2Built),
        new PieChart.Data(i18n.get("stats.tech3"), tech3Built)
    )));
  }

  @SuppressWarnings("unchecked")
  private void plotGamesPlayedChart(Map<String, PlayerEvent> playerEvents) {
    int tech1Built = playerEvents.containsKey(EVENT_CUSTOM_GAMES_PLAYED) ? playerEvents.get(EVENT_CUSTOM_GAMES_PLAYED).getCount() : 0;
    int tech2Built = playerEvents.containsKey(EVENT_RANKED_1V1_GAMES_PLAYED) ? playerEvents.get(EVENT_RANKED_1V1_GAMES_PLAYED).getCount() : 0;

    Platform.runLater(() -> gamesPlayedChart.setData(FXCollections.observableArrayList(
        new PieChart.Data(i18n.get("stats.custom"), tech1Built),
        new PieChart.Data(i18n.get("stats.ranked1v1"), tech2Built)
    )));
  }

  private void enterAchievementsLoadingState() {
    loadingProgressLabel.setVisible(true);
    achievementsPane.setVisible(false);
  }

  private void updatePlayerAchievements(List<? extends PlayerAchievement> playerAchievements) {
    PlayerAchievement mostRecentPlayerAchievement = null;
    int unlockedAchievements = 0;

    ObservableList<Node> children = unlockedAchievementsContainer.getChildren();
    Platform.runLater(children::clear);

    for (PlayerAchievement playerAchievement : playerAchievements) {
      AchievementItemController achievementItemController = achievementItemById.get(playerAchievement.getAchievementId());
      achievementItemController.setPlayerAchievement(playerAchievement);

      if (isUnlocked(playerAchievement)) {
        unlockedAchievements++;
        earnedExperiencePoints += achievementDefinitionById.get(playerAchievement.getAchievementId()).getExperiencePoints();
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
      AchievementDefinition mostRecentAchievement = achievementDefinitionById.get(mostRecentPlayerAchievement.getAchievementId());
      String mostRecentAchievementName = mostRecentAchievement.getName();
      String mostRecentAchievementDescription = mostRecentAchievement.getDescription();

      Platform.runLater(() -> {
        mostRecentAchievementNameLabel.setText(mostRecentAchievementName);
        mostRecentAchievementDescriptionLabel.setText(mostRecentAchievementDescription);
        mostRecentAchievementImageView.setImage(achievementService.getRevealedIcon(mostRecentAchievement));
      });
    }

    int totalExperiencePoints = 0;
    for (AchievementDefinition definition : achievementDefinitionById.values()) {
      totalExperiencePoints += definition.getExperiencePoints();
    }

    final int finalUnlockedAchievements = unlockedAchievements;
    final int finalTotalExperiencePoints = totalExperiencePoints;
    Platform.runLater(() -> {
      unlockedOfTotalLabel.setText(i18n.get("achievements.unlockedOfTotal",
          finalUnlockedAchievements, achievementDefinitionById.size()));
      pointsOfTotalLabel.setText(i18n.get("achievements.earnedOfTotal",
          earnedExperiencePoints, finalTotalExperiencePoints));
    });
  }

  private void enterAchievementsLoadedState() {
    loadingProgressLabel.setVisible(false);
    achievementsPane.setVisible(true);
  }

  private static boolean isUnlocked(PlayerAchievement playerAchievement) {
    return UNLOCKED.equals(playerAchievement.getState());
  }

  public Region getRoot() {
    return userInfoRoot;
  }

  @FXML
  void onRatingOver90DaysButtonClicked() {
    loadStatistics(StatisticsType.GLOBAL_90_DAYS);
  }

  private CompletableFuture<Void> loadStatistics(StatisticsType type) {
    return statisticsService.getStatisticsForPlayer(type, playerInfoBean.getUsername())
        .thenAccept(playerStatistics -> Platform.runLater(() -> plotPlayerRatingGraph(playerStatistics)))
        .exceptionally(throwable -> {
          // FIXME display to user
          logger.warn("Statistics could not be loaded", throwable);
          return null;
        });
  }

  @SuppressWarnings("unchecked")
  private void plotPlayerRatingGraph(PlayerStatisticsMessageLobby result) {
    XYChart.Series<Long, Integer> series = new XYChart.Series<>();
    series.setName(i18n.get("Player rating"));

    List<XYChart.Data<Long, Integer>> values = new ArrayList<>();

    for (RatingInfo ratingInfo : result.getValues()) {
      int minRating = RatingUtil.getRating(ratingInfo);
      LocalDateTime dateTime = LocalDate.from(ratingInfo.getDate()).atTime(ratingInfo.getTime());
      values.add(new XYChart.Data<>(dateTime.atZone(ZoneId.systemDefault()).toEpochSecond(), minRating));
    }

    series.getData().setAll(FXCollections.observableList(values));
    rating90DaysChart.getData().setAll(series);
  }

  @FXML
  void onRatingOver365DaysButtonClicked() {
    loadStatistics(StatisticsType.GLOBAL_365_DAYS);
  }
}
