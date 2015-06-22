package com.faforever.client.game;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.map.LegacyMapService;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
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

  private ObservableMap<Integer, GameInfoBean> gameInfoBeans;

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  LegacyMapService mapService;

  @Autowired
  I18n i18n;

  private void initializeGameTable() {
    gamesTable.setEditable(false);
    gameInfoBeans.addListener((MapChangeListener<Integer, GameInfoBean>) change -> {
      if (change.wasAdded()) {
        gamesTable.getItems().add(change.getValueAdded());
        if (gamesTable.getSelectionModel().getSelectedItem() == null) {
          gamesTable.getSelectionModel().select(0);
        }
      } else {
        gamesTable.getItems().remove(change.getValueRemoved());
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
        return StringUtils.defaultString(RatingUtil.extractRating(param.getValue().getTitle()));
      }
    });
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());

    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      // FIXME
//      displayGameDetail(newValue);
    });

    accessColumn.setCellValueFactory(param -> param.getValue().accessProperty());
  }
}
