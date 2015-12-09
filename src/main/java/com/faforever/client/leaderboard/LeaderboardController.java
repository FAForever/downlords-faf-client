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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static javafx.collections.FXCollections.observableArrayList;


public class LeaderboardController {

  @FXML
  Pane leaderboardRoot;
  @FXML
  TableColumn<Ranked1v1EntryBean, Number> rankColumn;
  @FXML
  TableColumn<Ranked1v1EntryBean, String> nameColumn;
  @FXML
  TableColumn<Ranked1v1EntryBean, Number> winLossColumn;
  @FXML
  TableColumn<Ranked1v1EntryBean, Number> gamesPlayedColumn;
  @FXML
  TableColumn<Ranked1v1EntryBean, Number> ratingColumn;
  @FXML
  TableView<Ranked1v1EntryBean> ratingTable;
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

  private List<Ranked1v1EntryBean> ranked1v1EntryBeans;
  private FilteredList<Ranked1v1EntryBean> filteredList;

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
        Ranked1v1EntryBean foundPlayer = null;
        for (Ranked1v1EntryBean ranked1v1EntryBean : ranked1v1EntryBeans) {
          if (ranked1v1EntryBean.getUsername().toLowerCase().startsWith(newValue.toLowerCase())) {
            foundPlayer = ranked1v1EntryBean;
            break;
          }
        }
        if (foundPlayer == null) {
          for (Ranked1v1EntryBean ranked1v1EntryBean : ranked1v1EntryBeans) {
            if (ranked1v1EntryBean.getUsername().toLowerCase().contains(newValue.toLowerCase())) {
              foundPlayer = ranked1v1EntryBean;
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
    leaderboardService.getLeaderboardEntries().thenAccept(leaderboardEntryBeans -> {
      LeaderboardController.this.ranked1v1EntryBeans = leaderboardEntryBeans;
      filteredList = new FilteredList<>(observableArrayList(leaderboardEntryBeans));
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
    return leaderboardRoot;
  }
}
