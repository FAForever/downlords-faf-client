package com.faforever.client.leaderboard;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.Validator;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static javafx.collections.FXCollections.observableArrayList;


public class LeaderboardController {

  @FXML
  TabPane ladderRoot;
  @FXML
  Tab ladderByRatingTab;
  @FXML
  Tab ladderByMapTab;
  @FXML
  Tab ladderByRankTab;
  @FXML
  TableColumn<LeaderboardEntryBean, Number> rankColumn;
  @FXML
  TableColumn<LeaderboardEntryBean, String> nameColumn;
  @FXML
  TableColumn<LeaderboardEntryBean, Number> winLossColumn;
  @FXML
  TableColumn<LeaderboardEntryBean, Number> gamesPlayedColumn;
  @FXML
  TableColumn<LeaderboardEntryBean, Number> ratingColumn;
  @FXML
  TableView<LeaderboardEntryBean> ratingTable;
  @FXML
  TextField searchTextField;

  @Resource
  LeaderboardService leaderboardService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  ReportingService reportingService;

  private List<LeaderboardEntryBean> leaderboardEntryBeans;

  private FilteredList<LeaderboardEntryBean> filteredList;

  @FXML
  public void initialize() {
    rankColumn.setCellValueFactory(param -> param.getValue().rankProperty());
    nameColumn.setCellValueFactory(param -> param.getValue().usernameProperty());
    winLossColumn.setCellValueFactory(param -> param.getValue().winLossRatioProperty());
    gamesPlayedColumn.setCellValueFactory(param -> param.getValue().gamesPlayedProperty());
    ratingColumn.setCellValueFactory(param -> param.getValue().ratingProperty());

    searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (Validator.isInt(newValue)) {
        ratingTable.scrollTo(Integer.parseInt(newValue) - 1);
      } else {
        LeaderboardEntryBean foundPlayer = null;
        for (LeaderboardEntryBean leaderboardEntryBean : leaderboardEntryBeans) {
          if (leaderboardEntryBean.getUsername().toLowerCase().startsWith(newValue.toLowerCase())) {
            foundPlayer = leaderboardEntryBean;
            break;
          }
        }
        if (foundPlayer == null) {
          for (LeaderboardEntryBean leaderboardEntryBean : leaderboardEntryBeans) {
            if (leaderboardEntryBean.getUsername().toLowerCase().contains(newValue.toLowerCase())) {
              foundPlayer = leaderboardEntryBean;
              break;
            }
          }
        }
        if (foundPlayer != null) {
          ratingTable.scrollTo(foundPlayer);
          ratingTable.getSelectionModel().select(foundPlayer);
        } else {
          ratingTable.getSelectionModel().select(null);
        }
      }
    });
  }

  public void setUpIfNecessary() {
    if (leaderboardEntryBeans != null) {
      return;
    }

    leaderboardService.getLeaderboardEntries().thenAccept(leaderboardEntryBeans1 -> {
      LeaderboardController.this.leaderboardEntryBeans = leaderboardEntryBeans1;
      filteredList = new FilteredList<>(observableArrayList(leaderboardEntryBeans1));
      ratingTable.setItems(filteredList);
    }).exceptionally(throwable -> {
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("errorTitle"), i18n.get("leaderboard.failedToLoad"),
          Severity.ERROR, throwable,
          Arrays.asList(
              new ReportAction(i18n, reportingService, throwable),
              new DismissAction(i18n)
          )
      ));
      return null;
    });
  }

  public Node getRoot() {
    return ladderRoot;
  }
}
