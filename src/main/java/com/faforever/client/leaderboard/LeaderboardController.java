package com.faforever.client.leaderboard;

import com.faforever.client.fx.StringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.Validator;
import javafx.beans.property.SimpleFloatProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import static javafx.collections.FXCollections.observableList;


public class LeaderboardController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
  @FXML
  Pane connectionProgressPane;
  @FXML
  Pane contentPane;

  @Resource
  LeaderboardService leaderboardService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  ReportingService reportingService;


  @FXML
  public void initialize() {
    rankColumn.setCellValueFactory(param -> param.getValue().rankProperty());
    nameColumn.setCellValueFactory(param -> param.getValue().usernameProperty());
    winLossColumn.setCellValueFactory(param -> new SimpleFloatProperty(param.getValue().getWinLossRatio()));
    winLossColumn.setCellFactory(param -> new StringCell<>(number -> i18n.get("percentage", number.floatValue() * 100)));
    gamesPlayedColumn.setCellValueFactory(param -> param.getValue().gamesPlayedProperty());
    ratingColumn.setCellValueFactory(param -> param.getValue().ratingProperty());

    contentPane.managedProperty().bind(contentPane.visibleProperty());
    connectionProgressPane.managedProperty().bind(connectionProgressPane.visibleProperty());
    connectionProgressPane.visibleProperty().bind(contentPane.visibleProperty().not());

    searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (Validator.isInt(newValue)) {
        ratingTable.scrollTo(Integer.parseInt(newValue) - 1);
      } else {
        Ranked1v1EntryBean foundPlayer = null;
        for (Ranked1v1EntryBean ranked1v1EntryBean : ratingTable.getItems()) {
          if (ranked1v1EntryBean.getUsername().toLowerCase().startsWith(newValue.toLowerCase())) {
            foundPlayer = ranked1v1EntryBean;
            break;
          }
        }
        if (foundPlayer == null) {
          for (Ranked1v1EntryBean ranked1v1EntryBean : ratingTable.getItems()) {
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
    contentPane.setVisible(false);
    leaderboardService.getRanked1v1Entries().thenAccept(leaderboardEntryBeans -> {
      ratingTable.setItems(observableList(leaderboardEntryBeans));
      contentPane.setVisible(true);
    }).exceptionally(throwable -> {
      contentPane.setVisible(false);
      logger.warn("Error while loading leaderboard entries", throwable);
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
