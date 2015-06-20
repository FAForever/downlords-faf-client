package com.faforever.client.leaderboard;

import com.faforever.client.util.Callback;
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


public class LadderController {

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
  TableColumn<LadderEntryBean, Number> rankColumn;

  @FXML
  TableColumn<LadderEntryBean, String> nameColumn;

  @FXML
  TableColumn<LadderEntryBean, Number> winLossColumn;

  @FXML
  TableColumn<LadderEntryBean, Number> gamesPlayedColumn;

  @FXML
  TableColumn<LadderEntryBean, Number> ratingColumn;

  @FXML
  TableView<LadderEntryBean> ratingTable;

  @FXML
  TextField searchTextField;

  @Autowired
  LadderService ladderService;

  private List<LadderEntryBean> ladderEntryBeans;

  private FilteredList<LadderEntryBean> filteredList;

  @FXML
  public void initialize() {
    rankColumn.setCellValueFactory(param -> param.getValue().rankProperty());
    nameColumn.setCellValueFactory(param -> param.getValue().usernameProperty());
    winLossColumn.setCellValueFactory(param -> param.getValue().winLossRatioProperty());
    gamesPlayedColumn.setCellValueFactory(param -> param.getValue().gamesPlayedProperty());
    ratingColumn.setCellValueFactory(param -> param.getValue().ratingProperty());

    searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      filteredList.setPredicate(ladderEntryBean -> usernameOrRankingPredicate(newValue, ladderEntryBean));
    });
  }

  private boolean usernameOrRankingPredicate(String newValue, LadderEntryBean ladderEntryBean) {
    return ladderEntryBean.getUsername().toLowerCase().contains(newValue)
        || String.valueOf(ladderEntryBean.getRank()).startsWith(newValue);
  }

  public void setUpIfNecessary() {
    if (ladderEntryBeans != null) {
      return;
    }

    ladderService.getLadderInfo(new Callback<List<LadderEntryBean>>() {
      @Override
      public void success(List<LadderEntryBean> result) {
        ladderEntryBeans = result;
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
