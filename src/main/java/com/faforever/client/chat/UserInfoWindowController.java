package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.play.AchievementDefinition;
import com.faforever.client.play.PlayServices;
import com.faforever.client.play.PlayerAchievement;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.user.AchievementItemController;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.MoreObjects;
import com.neovisionaries.i18n.CountryCode;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.play.AchievementState.UNLOCKED;

public class UserInfoWindowController {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM");
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  ImageView mostRecentAchievementImageView;
  @FXML
  ProgressIndicator loadingProgressIndicator;
  @FXML
  Label loadingProgressLabel;
  @FXML
  Label achievementsProgressLabel;
  @FXML
  Pane mostRecentAchievementPane;
  @FXML
  Label mostRecentAchievementNameLabel;
  @FXML
  VBox availableAchievementsContainer;
  @FXML
  VBox achievedAchievementsContainer;
  @FXML
  Label notUsingClientLabel;
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
  PlayServices playServices;
  @Resource
  PreferencesService preferencesService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  I18n i18n;

  private PlayerInfoBean playerInfoBean;
  private Map<String, AchievementItemController> achievementItemById;
  private Map<String, AchievementDefinition> achievementDefinitionById;

  public UserInfoWindowController() {
    achievementItemById = new HashMap<>();
    achievementDefinitionById = new HashMap<>();
  }

  @FXML
  void initialize() {
    loadingProgressLabel.managedProperty().bind(loadingProgressLabel.visibleProperty());
    loadingProgressIndicator.managedProperty().bind(loadingProgressIndicator.visibleProperty());
    loadingProgressIndicator.visibleProperty().bind(loadingProgressLabel.visibleProperty());
    mostRecentAchievementPane.managedProperty().bind(mostRecentAchievementPane.visibleProperty());
    achievedAchievementsContainer.managedProperty().bind(achievedAchievementsContainer.visibleProperty());
    availableAchievementsContainer.managedProperty().bind(availableAchievementsContainer.visibleProperty());
    notUsingClientLabel.managedProperty().bind(notUsingClientLabel.visibleProperty());

    enterAchievementsLoadedState();

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

  private void enterAchievementsLoadedState() {
    loadingProgressLabel.setVisible(false);
    mostRecentAchievementPane.setVisible(true);
    achievedAchievementsContainer.setVisible(true);
    availableAchievementsContainer.setVisible(true);
  }

  private void displayAvailableAchievements(List<AchievementDefinition> achievementDefinitions) {
    if (achievementDefinitions == null) {
      notUsingClientLabel.setVisible(true);
      return;
    }

    ObservableList<Node> children = availableAchievementsContainer.getChildren();
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
  }

  private void loadAchievements() {
    playServices.authorize().thenRun(() -> {
      enterAchievementsLoadingState();
      playServices.getAchievementDefinitions()
          .exceptionally(throwable -> {
            logger.warn("Could not authorize to play services", throwable);
            return null;
          })
          .thenAccept(this::displayAvailableAchievements)
          .thenRun(() -> {
            ObservableList<PlayerAchievement> playerAchievements = playServices.getPlayerAchievements(playerInfoBean.getUsername());
            playerAchievements.addListener((Observable observable) -> {
              updatePlayerAchievements(playerAchievements);
            });
            updatePlayerAchievements(playerAchievements);
            enterAchievementsLoadedState();
          });
    }).exceptionally(throwable -> {
      logger.warn("Error while loading achievements", throwable);
      return null;
    });
  }

  private void enterAchievementsLoadingState() {
    loadingProgressLabel.setVisible(true);
    mostRecentAchievementPane.setVisible(false);
    achievedAchievementsContainer.setVisible(false);
    availableAchievementsContainer.setVisible(false);
    notUsingClientLabel.setVisible(false);
  }

  private void updatePlayerAchievements(List<? extends PlayerAchievement> playerAchievements) {
    PlayerAchievement mostRecentPlayerAchievement = null;
    int unlockedAchievements = 0;

    ObservableList<Node> children = achievedAchievementsContainer.getChildren();
    Platform.runLater(children::clear);

    for (PlayerAchievement playerAchievement : playerAchievements) {
      AchievementItemController achievementItemController = achievementItemById.get(playerAchievement.getAchievementId());
      achievementItemController.setPlayerAchievement(playerAchievement);

      if (isUnlocked(playerAchievement)) {
        unlockedAchievements++;
        Platform.runLater(() -> children.add(achievementItemController.getRoot()));
        if (mostRecentPlayerAchievement == null
            || playerAchievement.getUpdateTime().compareTo(mostRecentPlayerAchievement.getUpdateTime()) > 0) {
          mostRecentPlayerAchievement = playerAchievement;
        }
      }
    }

    int numberOfAchievements = achievementDefinitionById.size();
    double percentageUnlocked = (double) unlockedAchievements / numberOfAchievements;
    String achievementProgressText = i18n.get("achievements.unlockedTotal", unlockedAchievements, numberOfAchievements, percentageUnlocked);
    Platform.runLater(() -> achievementsProgressLabel.setText(achievementProgressText));

    if (mostRecentPlayerAchievement == null) {
      mostRecentAchievementPane.setVisible(false);
    } else {
      mostRecentAchievementPane.setVisible(true);
      AchievementDefinition mostRecentAchievement = achievementDefinitionById.get(mostRecentPlayerAchievement.getAchievementId());
      String mostRecentAchievementName = mostRecentAchievement.getName();

      // TODO use proper image
      String imageUrl = MoreObjects.firstNonNull(
          mostRecentAchievement.getUnlockedIconUrl(),
          getClass().getResource("/images/tray_icon.png").toString()
      );

      Platform.runLater(() -> {
        mostRecentAchievementNameLabel.setText(mostRecentAchievementName);
        mostRecentAchievementImageView.setImage(new Image(imageUrl, true));
      });
    }
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
        .thenAccept(playerStatistics -> Platform.runLater(() -> plotPlayerStatistics(playerStatistics)))
        .exceptionally(throwable -> {
          // FIXME display to user
          logger.warn("Statistics could not be loaded", throwable);
          return null;
        });
  }

  @SuppressWarnings("unchecked")
  private void plotPlayerStatistics(PlayerStatistics result) {
    XYChart.Series<Long, Integer> series = new XYChart.Series<>();
    // FIXME i18n
    series.setName("Player rating");

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
