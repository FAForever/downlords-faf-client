package com.faforever.client.chat;

import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.util.Callback;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserInfoWindowController {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM");

  @FXML
  NumberAxis rating90DaysXAxis;

  @FXML
  LineChart<Long, Float> rating90DaysChart;

  @FXML
  ImageView countryFlagImageView;

  @FXML
  Label usernameLabel;

  @FXML
  Region userInfoRoot;

  @Autowired
  StatisticsService statisticsService;

  @Autowired
  CountryFlagService countryFlagService;

  private PlayerInfoBean playerInfoBean;

  public void setPlayerInfoBean(PlayerInfoBean playerInfoBean) {
    this.playerInfoBean = playerInfoBean;

    usernameLabel.textProperty().bind(playerInfoBean.usernameProperty());
    countryFlagImageView.setImage(countryFlagService.loadCountryFlag(playerInfoBean.getCountry()));

    statisticsService.getStatisticsForPlayer(playerInfoBean.getUsername(), new Callback<PlayerStatistics>() {
      @Override
      public void success(PlayerStatistics result) {
        plotPlayerStatistics(result);
      }

      @Override
      public void error(Throwable e) {
        // FIXME implement
      }
    });
  }

  private void plotPlayerStatistics(PlayerStatistics result) {
    XYChart.Series<Long, Float> series = new XYChart.Series<>();
    // FIXME i18n
    series.setName("Player rating");

    List<XYChart.Data<Long, Float>> values = new ArrayList<>();

    for (RatingInfo ratingInfo : result.values) {
      float minRating = ratingInfo.mean - 3 * ratingInfo.dev;
      LocalDateTime dateTime = LocalDate.from(ratingInfo.date).atTime(ratingInfo.time);
      values.add(new XYChart.Data<>(dateTime.atZone(ZoneId.systemDefault()).toEpochSecond(), minRating));
    }

    rating90DaysXAxis.setForceZeroInRange(false);
    rating90DaysXAxis.setAutoRanging(true);
    rating90DaysXAxis.setTickLabelFormatter(new StringConverter<Number>() {
      @Override
      public String toString(Number object) {
        return DATE_FORMATTER.format(Instant.ofEpochSecond(object.longValue()));
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
}
