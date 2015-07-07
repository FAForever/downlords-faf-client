package com.faforever.client.game;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.map.MapService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Strings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

public class GameTableController {


  @FXML
  TableView<GameInfoBean> gamesTable;

  @FXML
  TableColumn<GameInfoBean, Image> mapPreviewColumn;

  @FXML
  TableColumn<GameInfoBean, String> gameTitleColumn;

  @FXML
  TableColumn<GameInfoBean, String> playersColumn;

  @FXML
  TableColumn<GameInfoBean, String> rankingColumn;

  @FXML
  TableColumn<GameInfoBean, String> hostColumn;

  @FXML
  TableColumn<GameInfoBean, GameAccess> accessColumn;

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  MapService mapService;

  //FIXME replace with gamecontroller listener
  @Autowired
  GamesController gamesController;

  @Autowired
  I18n i18n;

  private double lastMouseX;
  private double lastMouseY;

  public void initializeGameTable(ObservableList<GameInfoBean> gameInfoBeans) {
    SortedList<GameInfoBean> sortedList = new SortedList<>(gameInfoBeans);
    sortedList.comparatorProperty().bind(gamesTable.comparatorProperty());
    gamesTable.setItems(sortedList);
    gamesTable.setRowFactory(param1 -> gamesRowFactory());

    gameInfoBeans.addListener((ListChangeListener<GameInfoBean>) change -> {
      while (change.next()) {
        if (change.wasAdded() && gamesTable.getSelectionModel().getSelectedItem() == null) {
          gamesTable.getSelectionModel().select(0);
        }
      }
    });

    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(fxmlLoader));
    mapPreviewColumn.setCellValueFactory(param -> new ObjectBinding<Image>() {
      @Override
      protected Image computeValue() {
        return mapService.loadSmallPreview(param.getValue().getMapName());
      }
    });

    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    playersColumn.setCellValueFactory(param -> new NumberOfPlayersBinding(i18n, param.getValue().numPlayersProperty(), param.getValue().maxPlayersProperty()));
    rankingColumn.setCellValueFactory(param -> new StringBinding() {
      @Override
      protected String computeValue() {
        // TODO this is not bound to the title property, however, a game's title can't be changed anyway (atm).
        return Strings.nullToEmpty(RatingUtil.extractRating(param.getValue().getTitle()));
      }
    });
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());

    //TODO fix null newValues while gameInfoBean isEmpty
    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null && !gameInfoBeans.isEmpty()) {
        gamesTable.getSelectionModel().select(gameInfoBeans.get(0));
      } else {
        gamesController.displayGameDetail(newValue);
      }
    });

    accessColumn.setCellValueFactory(param -> param.getValue().accessProperty());
  }

  @NotNull
  private TableRow<GameInfoBean> gamesRowFactory() {
    TableRow<GameInfoBean> row = new TableRow<>();
    row.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        lastMouseX = event.getScreenX();
        lastMouseY = event.getScreenY();
        gamesController.joinGame(row.getItem(), null, lastMouseX, lastMouseY);
      }
    });
    return row;
  }

  public Node getRoot() {
    return gamesTable;
  }
}
