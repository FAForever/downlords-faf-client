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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.RangeSlider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.Objects;

public class CreateGameController {

  @FXML
  Label mapNameLabel;

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
        int newMapIndex = currentMapIndex;
        if (KeyCode.DOWN == event.getCode()) {
          if (filteredMaps.size() > currentMapIndex + 1) {
            newMapIndex++;
          }
          event.consume();
        } else if (KeyCode.UP == event.getCode()) {
          if (currentMapIndex > 0) {
            newMapIndex--;
          }
          event.consume();
        }
        selectionModel.select(newMapIndex);
        mapListView.scrollTo(newMapIndex);
      }
    });

    gameTypeComboBox.setCellFactory(param -> new ModListCell());
    gameTypeComboBox.setButtonCell(new ModListCell());
  }

  @PostConstruct
  void postConstruct() {
    initRankingSlider();
    initMapSelection();
    initModList();
    initGameTypeComboBox();
    initLatestGameSettings();
  }

  private void initLatestGameSettings() {
    titleTextField.setText(preferencesService.getPreferences().getLatestGameTitle());
  }

  private void initGameTypeComboBox() {
    gameService.addOnGameTypeInfoListener(change -> {
      change.getValueAdded();

      gameTypeComboBox.getItems().add(change.getValueAdded());
      selectLatestGameType();
    });
  }

  private void selectLatestGameType() {
    String lastGameMod = preferencesService.getPreferences().getLatestGameType();
    for (GameTypeBean mod : gameTypeComboBox.getItems()) {
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
        mapNameLabel.setText("");
        return;
      }
      mapNameLabel.setText(newValue.getName());
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

    titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLatestGameTitle(newValue);
      preferencesService.storeInBackground();
    });
  }

  @FXML
  void onRandomMapButtonClicked(ActionEvent event) {
    int mapIndex = (int) (Math.random() * filteredMaps.size());
    mapListView.getSelectionModel().select(mapIndex);
    mapListView.scrollTo(mapIndex);
  }

  @FXML
  void onCreateButtonClicked(ActionEvent event) {
    if (StringUtils.isEmpty(titleTextField.getText())) {
      // TODO tell the user
      return;
    }

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
