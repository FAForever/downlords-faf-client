package com.faforever.client.chat;

import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.RatingUtil;
import com.neovisionaries.i18n.CountryCode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserInfoWindowController {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM");

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  @Autowired
  StatisticsService statisticsService;

  @Autowired
  CountryFlagService countryFlagService;

  private PlayerInfoBean playerInfoBean;

  @PostConstruct
  void postConstruct() {
    ratingOver90DaysButton.setSelected(true);
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
  }

  private void plotPlayerStatistics(PlayerStatistics result) {
    XYChart.Series<Long, Integer> series = new XYChart.Series<>();
    // FIXME i18n
    series.setName("Player rating");

    List<XYChart.Data<Long, Integer>> values = new ArrayList<>();

    for (RatingInfo ratingInfo : result.values) {
      int minRating = RatingUtil.getRating(ratingInfo);
      LocalDateTime dateTime = LocalDate.from(ratingInfo.date).atTime(ratingInfo.time);
      values.add(new XYChart.Data<>(dateTime.atZone(ZoneId.systemDefault()).toEpochSecond(), minRating));
    }

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

    series.getData().setAll(FXCollections.observableList(values));
    rating90DaysChart.getData().add(series);
  }

  public Region getUserInfoRoot() {
    return userInfoRoot;
  }

  @FXML
  void onRatingOver90DaysButtonClicked(ActionEvent event) {
    loadStatistics(StatisticsType.GLOBAL_90_DAYS);
  }

  @FXML
  void onRatingOver365DaysButtonClicked(ActionEvent event) {
    loadStatistics(StatisticsType.GLOBAL_365_DAYS);
  }

  private void loadStatistics(StatisticsType type) {
    statisticsService.getStatisticsForPlayer(StatisticsType.GLOBAL_365_DAYS, playerInfoBean.getUsername(), new Callback<PlayerStatistics>() {
      @Override
      public void success(PlayerStatistics result) {
        Platform.runLater(() -> plotPlayerStatistics(result));
      }

      @Override
      public void error(Throwable e) {
        // FIXME display to user
        logger.warn("Statistics could not be loaded", e);
      }
    });
  }
}