package com.faforever.client.game;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.map.MapService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Strings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.Rating;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Observable;

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

  @Autowired
  GamesController gamesController;

  @Autowired
  I18n i18n;

  private FilteredList<GameInfoBean> filteredItems;

  public void initializeGameTable(ObservableMap<Integer, GameInfoBean> gameInfoBeans) {
    ObservableList<GameInfoBean> tableItems = FXCollections.observableArrayList();
    if (gamesController.isFirstGeneratedPane()) {
      filteredItems = new FilteredList<>(tableItems);
      gamesController.setFilteredList(filteredItems);
    } else {
      filteredItems = gamesController.returnFilteredList();
    }
    gamesTable.setItems(filteredItems);
    //FIXME possible bug may not update password protected games
    tableItems.addAll(gameInfoBeans.values());

    gameInfoBeans.addListener((MapChangeListener<Integer, GameInfoBean>) change -> {
      if (change.wasAdded()) {
        tableItems.add(change.getValueAdded());
        if (gamesTable.getSelectionModel().getSelectedItem() == null) {
          gamesTable.getSelectionModel().select(0);
        }
      } else {
        tableItems.remove(change.getValueRemoved());
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

    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null && !filteredItems.isEmpty()) {
        gamesTable.getSelectionModel().select(filteredItems.get(0));
      } else {
        gamesController.displayGameDetail(newValue);
      }
    });

    accessColumn.setCellValueFactory(param -> param.getValue().accessProperty());
  }

  @FXML
  void onTableClicked(MouseEvent event) {
    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
      GameInfoBean gameInfoBean = gamesTable.getSelectionModel().getSelectedItem();
      gamesController.joinSelectedGame(null, gameInfoBean, event);
    }
  }

  public Node getRoot() {
    return gamesTable;
  }
}
