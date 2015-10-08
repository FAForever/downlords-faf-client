package com.faforever.client.chat;

import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.play.PlayServices;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.user.AchievementItemController;
import com.faforever.client.util.RatingUtil;
import com.google.api.services.games.model.AchievementDefinition;
import com.google.api.services.games.model.PlayerAchievement;
import com.neovisionaries.i18n.CountryCode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Nullable;
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

public class UserInfoWindowController {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM");
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  Label notUsingClientLabel;
  @FXML
  Label connectToGoogleLabel;
  @FXML
  Button connectToGoogleButton;
  @FXML
  Pane achievementsLayout;
  @FXML
  Pane noAchievementsLayout;
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
  PlayerService playerService;
  @Resource
  ApplicationContext applicationContext;

  private PlayerInfoBean playerInfoBean;
  private Map<String, AchievementItemController> achievementItemById;

  public UserInfoWindowController() {
    achievementItemById = new HashMap<>();
  }

  @FXML
  void initialize() {
    connectToGoogleButton.managedProperty().bindBidirectional(connectToGoogleButton.visibleProperty());
    connectToGoogleLabel.managedProperty().bindBidirectional(connectToGoogleLabel.visibleProperty());
    notUsingClientLabel.managedProperty().bindBidirectional(notUsingClientLabel.visibleProperty());

    rating90DaysYAxis.setForceZeroInRange(false);
    rating90DaysYAxis.setAutoRanging(true);

    rating90DaysXAxis.setForceZeroInRange(false);
    rating90DaysXAxis.setAutoRanging(true);
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

  private void loadAchievements() {
    if (!preferencesService.getPreferences().getConnectedToGooglePlay()) {
      preferencesService.getPreferences().connectedToGooglePlayProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue) {
          loadAchievements();
        }
      });
      notUsingClientLabel.setVisible(false);
      connectToGoogleButton.setVisible(true);
      connectToGoogleLabel.setVisible(true);
      return;
    }

    connectToGoogleButton.setVisible(false);
    connectToGoogleLabel.setVisible(false);

    playServices.authorize().thenRun(() -> playServices.getAchievementDefinitions()
        .thenAccept(UserInfoWindowController.this::displayAvailableAchievements)
        .exceptionally(throwable -> {
          logger.warn("Error while loading achievement definitions", throwable);
          return null;
        })
        .thenRun(() -> playServices.getAchievements(playerInfoBean.getUsername())
            .thenAccept(UserInfoWindowController.this::displayAchievements)
            .exceptionally(throwable -> {
              logger.warn("Error while loading achievements", throwable);
              return null;
            }))
        .exceptionally(throwable -> {
          logger.warn("Error while loading player achievements", throwable);
          return null;
        }));
  }

  private void displayAvailableAchievements(List<AchievementDefinition> achievementDefinitions) {
    ObservableList<Node> children = achievementsLayout.getChildren();

    Platform.runLater(children::clear);

    for (AchievementDefinition achievementDefinition : achievementDefinitions) {
      AchievementItemController controller = applicationContext.getBean(AchievementItemController.class);
      controller.setAchievementDefinition(achievementDefinition);
      achievementItemById.put(achievementDefinition.getId(), controller);
      Platform.runLater(() -> children.add(controller.getRoot()));
    }
  }

  private void displayAchievements(@Nullable List<PlayerAchievement> playerAchievements) {
    if (playerAchievements == null) {
      notUsingClientLabel.setVisible(true);
      return;
    }
    notUsingClientLabel.setVisible(false);
    playerAchievements.forEach(item -> achievementItemById.get(item.getId()).setPlayerAchievement(item));
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

  @FXML
  void onConnectToGoogleButtonClicked() {
    playServices.authorize().whenComplete((aVoid, throwable) -> {
      preferencesService.getPreferences().setConnectedToGooglePlay(throwable == null);
      preferencesService.storeInBackground();
    });
  }
}
