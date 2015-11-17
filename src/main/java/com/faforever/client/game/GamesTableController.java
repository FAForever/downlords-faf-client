package com.faforever.client.game;

import com.faforever.client.fx.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import javafx.application.Platform;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

public class GamesTableController {


  @FXML
  TableView<GameInfoBean> gamesTable;

  @FXML
  TableColumn<GameInfoBean, Image> mapPreviewColumn;

  @FXML
  TableColumn<GameInfoBean, String> gameTitleColumn;

  @FXML
  TableColumn<GameInfoBean, String> playersColumn;

  @FXML
  TableColumn<GameInfoBean, RatingRange> ratingColumn;

  @FXML
  TableColumn<GameInfoBean, String> hostColumn;

  @FXML
  TableColumn<GameInfoBean, Boolean> passwordColumn;

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  MapService mapService;

  // TODO replace with gamecontroller listener
  @Autowired
  GamesController gamesController;

  @Autowired
  I18n i18n;

  public void initializeGameTable(ObservableList<GameInfoBean> gameInfoBeans) {
    SortedList<GameInfoBean> sortedList = new SortedList<>(gameInfoBeans);
    sortedList.comparatorProperty().bind(gamesTable.comparatorProperty());
    gamesTable.setItems(sortedList);
    gamesTable.setRowFactory(param1 -> gamesRowFactory());

    gameInfoBeans.addListener((ListChangeListener<GameInfoBean>) change -> {
      while (change.next()) {
        if (change.wasAdded() && gamesTable.getSelectionModel().getSelectedItem() == null) {
          Platform.runLater(() -> gamesTable.getSelectionModel().select(0));
        }
      }
    });

    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(fxmlLoader));
    mapPreviewColumn.setCellValueFactory(param -> new ObjectBinding<Image>() {
      @Override
      protected Image computeValue() {
        return mapService.loadSmallPreview(param.getValue().getTechnicalName());
      }

      {
        bind(param.getValue().technicalNameProperty());
      }
    });

    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    playersColumn.setCellValueFactory(param -> new NumberOfPlayersBinding(i18n, param.getValue().numPlayersProperty(), param.getValue().maxPlayersProperty()));
    ratingColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(new RatingRange(param.getValue().getMinRating(), param.getValue().getMaxRating())));
    ratingColumn.setCellFactory(param -> ratingTableCell());
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());

    //TODO fix null newValues while gameInfoBean isEmpty
    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null && !gamesTable.getItems().isEmpty()) {
        gamesTable.getSelectionModel().select(gamesTable.getItems().get(0));
      } else {
        Platform.runLater(() -> gamesController.displayGameDetail(newValue));
      }
    });

    passwordColumn.setCellValueFactory(param -> param.getValue().passwordProtectedProperty());
  }

  @NotNull
  private TableRow<GameInfoBean> gamesRowFactory() {
    TableRow<GameInfoBean> row = new TableRow<>();
    row.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        GameInfoBean gameInfoBean = row.getItem();
        gamesController.onJoinGame(gameInfoBean, null, event.getScreenX(), event.getScreenY());
      }
    });
    return row;
  }

  private TableCell<GameInfoBean, RatingRange> ratingTableCell() {
    return new TableCell<GameInfoBean, RatingRange>() {
      @Override
      protected void updateItem(RatingRange item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          if (item.getMin() == null && item.getMax() == null) {
            setText("");
            return;
          }

          if (item.getMin() != null && item.getMax() != null) {
            setText(i18n.get("game.ratingFormat.minMax", item.getMin(), item.getMax()));
            return;
          }

          if (item.getMin() != null) {
            setText(i18n.get("game.ratingFormat.minOnly", item.getMin()));
            return;
          }

          setText(i18n.get("game.ratingFormat.maxOnly", item.getMax()));
        }
      }
    };
  }

  public Node getRoot() {
    return gamesTable;
  }
}
