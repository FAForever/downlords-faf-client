package com.faforever.client.game;

import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.RangeSlider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

public class CreateGameController {

  @FXML
  TextField mapSearchTextField;

  @FXML
  ImageView mapImageView;

  @FXML
  TextField titleTextField;

  @FXML
  CheckListView<GameTypeBean> modListView;

  @FXML
  TextField passwordTextField;

  @FXML
  TextField minRankingTextField;

  @FXML
  RangeSlider rankingSlider;

  @FXML
  TextField maxRankingTextField;

  @FXML
  ComboBox<GameTypeBean> gameTypeComboBox;

  @FXML
  ListView<MapInfoBean> mapListView;

  @Autowired
  Environment environment;

  @Autowired
  MapService mapService;

  @Autowired
  ModService modService;

  @Autowired
  GameService gameService;

  @Autowired
  PreferencesService preferencesService;

  @FXML
  Node createGameRoot;

  private FilteredList<MapInfoBean> filteredMaps;

  @FXML
  void initialize() {
    mapSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.isEmpty()) {
        filteredMaps.setPredicate(mapInfoBean -> true);
      } else {
        filteredMaps.setPredicate(mapInfoBean -> mapInfoBean.getName().toLowerCase().contains(newValue.toLowerCase()));
      }
      if (!filteredMaps.isEmpty()) {
        mapListView.getSelectionModel().select(0);
      }
    });
    mapSearchTextField.setOnKeyPressed(new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        MultipleSelectionModel<MapInfoBean> selectionModel = mapListView.getSelectionModel();
        int currentMapIndex = selectionModel.getSelectedIndex();
        if (KeyCode.DOWN == event.getCode()) {
          if (filteredMaps.size() > currentMapIndex + 1) {
            selectionModel.select(++currentMapIndex);
          }
        } else if (KeyCode.UP == event.getCode()) {
          if (currentMapIndex > 0) {
            selectionModel.select(--currentMapIndex);
          }
        }
        event.consume();
      }
    });
  }

  @PostConstruct
  void postConstruct() {
    initRankingSlider();
    initMapSelection();
    initModList();
    initGameTypeComboBox();
  }

  private void initGameTypeComboBox() {
    List<GameTypeBean> gameTypes = gameService.getGameTypes();

    gameTypeComboBox.getItems().setAll(gameTypes);
    gameTypeComboBox.setCellFactory(param -> new ModListCell());

    String lastGameMod = preferencesService.getPreferences().getLastGameMod();
    for (GameTypeBean mod : gameTypes) {
      if (Objects.equals(mod.getName(), lastGameMod)) {
        gameTypeComboBox.getSelectionModel().select(mod);
        break;
      }
    }
  }

  private void initModList() {
    modListView.setItems(modService.getInstalledMods());
    modListView.setCellFactory(param -> new ModListCell());
  }

  private void initMapSelection() {
    ObservableList<MapInfoBean> localMaps = mapService.getLocalMaps();

    filteredMaps = new FilteredList<>(localMaps);

    mapListView.setItems(filteredMaps);
    mapListView.setCellFactory(param -> new MapListCell());
    mapListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      mapImageView.setImage(mapService.loadLargePreview(newValue.getName()));
    });
    // FIXME use latest hosted map
    mapListView.getSelectionModel().select(0);
  }

  private void initRankingSlider() {
    rankingSlider.setFocusTraversable(false);
    rankingSlider.lowValueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.intValue() < rankingSlider.getHighValue()) {
        minRankingTextField.setText(String.valueOf(newValue.intValue()));
      }
    });
    rankingSlider.highValueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.intValue() > rankingSlider.getLowValue()) {
        maxRankingTextField.setText(String.valueOf(newValue.intValue()));
      }
    });
    minRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      rankingSlider.setLowValue(Double.parseDouble(newValue));
    });
    maxRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      rankingSlider.setHighValue(Double.parseDouble(newValue));
    });

    rankingSlider.setMax(environment.getProperty("rating.max", Integer.class));
    rankingSlider.setMin(environment.getProperty("rating.min", Integer.class));
    rankingSlider.setHighValue(environment.getProperty("rating.selectedMax", Integer.class));
    rankingSlider.setLowValue(environment.getProperty("rating.selectedMin", Integer.class));
  }

  @FXML
  void onRandomMapButtonClicked(ActionEvent event) {
    int mapIndex = (int) (Math.random() * filteredMaps.size());
    mapListView.getSelectionModel().select(mapIndex);
  }

  @FXML
  void onCreateButtonClicked(ActionEvent event) {
    NewGameInfo newGameInfo = new NewGameInfo(
        titleTextField.getText(),
        passwordTextField.getText(),
        gameTypeComboBox.getSelectionModel().getSelectedItem().getName(),
        mapListView.getSelectionModel().getSelectedItem().getName(),
        0);

    gameService.hostGame(newGameInfo, new Callback<Void>() {
          @Override
          public void success(Void result) {
            // FIXME do something or remove the callback
          }

          @Override
          public void error(Throwable e) {
            // FIXME do something or remove the callback
          }
        }
    );
  }

  public Node getRoot() {
    return createGameRoot;
  }
}
