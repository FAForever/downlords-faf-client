package com.faforever.client.leaderboard;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.game.GameInfoBean;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
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
  TableColumn<LadderInfoBean, Number> rankColumn;

  @FXML
  TableColumn<LadderInfoBean, String> nameColumn;

  @FXML
  TableColumn<LadderInfoBean, Number> winLossColumn;

  @FXML
  TableColumn<LadderInfoBean, Number> gamesPlayedColumn;

  @FXML
  TableColumn<LadderInfoBean, Number> ratingColumn;

  @FXML
  Slider ratingSlider;

  @FXML
  TableView<LadderInfoBean> ratingTable;

  @Autowired
  LadderService ladderService;

  private List<LadderInfoBean> ladderInfo;

  @PostConstruct
  public void postConstruct() {
    ladderInfo = ladderService.getLadderInfo();

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
    displayPage(ratingSlider.getValue());
    ratingSlider.setLabelFormatter(new StringConverter<Double>() {
      @Override
      public String toString(Double value) {
        int thousands = (int) (value / 1000);
        return String.valueOf(thousands * 1000);
      }

      @Override
      public Double fromString(String string) {
        return null;
      }
    });

  }

  /**
   * @param minRanking starting at 1
   */
  private void displayPage(Number minRanking) {
    ratingSlider.setMax(ladderInfo.size());

    int fromIndex = minRanking.intValue() - 1;
    int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, ladderInfo.size());
    ratingTable.setItems(FXCollections.observableArrayList(ladderInfo.subList(fromIndex, toIndex)));
  }


  public Node getRoot() {
    return ladderRoot;
  }
}
