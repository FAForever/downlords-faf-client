package com.faforever.client.leaderboard;

import com.faforever.client.util.Callback;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


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
  Slider ratingSlider;

  @FXML
  TableView<LadderEntryBean> ratingTable;

  @Autowired
  LadderService ladderService;

  private List<LadderEntryBean> ladderEntryBeans;

  @FXML
  public void initialize() {
    rankColumn.setCellValueFactory(param -> param.getValue().rankProperty());
    nameColumn.setCellValueFactory(param -> param.getValue().usernameProperty());
    winLossColumn.setCellValueFactory(param -> param.getValue().winLossRatioProperty());
    gamesPlayedColumn.setCellValueFactory(param -> param.getValue().gamesPlayedProperty());
    ratingColumn.setCellValueFactory(param -> param.getValue().ratingProperty());

    ratingSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.intValue() != oldValue.intValue()) {
        displayPage(newValue.intValue());
      }
    });
    ratingSlider.setLabelFormatter(new StringConverter<Double>() {
      @Override
      public String toString(Double value) {
        if(value == 0d){
          return "1";
        }
        return String.valueOf(value.intValue());
      }

      @Override
      public Double fromString(String string) {
        return null;
      }
    });
  }

  public void setUp() {
    ladderService.getLadderInfo(new Callback<List<LadderEntryBean>>() {
      @Override
      public void success(List<LadderEntryBean> result) {
        ladderEntryBeans = result;
        displayPage((int) ratingSlider.getValue());
      }

      @Override
      public void error(Throwable e) {
        // FIXME implement
      }
    });
  }

  /**
   * @param fromIndex starting at 0
   */
  private void displayPage(int fromIndex) {
    ratingSlider.setMax(Math.max(1, ladderEntryBeans.size()));

    int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, ladderEntryBeans.size());
    ratingTable.setItems(FXCollections.observableArrayList(ladderEntryBeans.subList(fromIndex, toIndex)));
  }


  public Node getRoot() {
    return ladderRoot;
  }
}
