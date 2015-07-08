package com.faforever.client.leaderboard;

import com.faforever.client.util.Callback;
import com.faforever.client.util.Validator;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static javafx.collections.FXCollections.observableArrayList;


public class LeaderboardController {

  private static final Integer ROWS_PER_PAGE = 1000;

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

  @Autowired
  LeaderboardService leaderboardService;

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

    leaderboardService.getLadderInfo(new Callback<List<LeaderboardEntryBean>>() {
      @Override
      public void success(List<LeaderboardEntryBean> result) {
        leaderboardEntryBeans = result;
        filteredList = new FilteredList<>(observableArrayList(result));
        ratingTable.setItems(filteredList);
      }

      @Override
      public void error(Throwable e) {
        // FIXME implement
      }
    });
  }

  public Node getRoot() {
    return ladderRoot;
  }
}
