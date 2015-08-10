package com.faforever.client.game;

import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.RangeSlider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

public class CreateGameController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  Label mapNameLabel;

  @FXML
  TextField mapSearchTextField;

  @FXML
  ImageView mapImageView;

  @FXML
  TextField titleTextField;

  @FXML
  CheckListView<ModInfoBean> modListView;

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

  @VisibleForTesting
  FilteredList<MapInfoBean> filteredMaps;

  @FXML
  void initialize() {
    mapSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.isEmpty()) {
        filteredMaps.setPredicate(mapInfoBean -> true);
      } else {
        filteredMaps.setPredicate(mapInfoBean -> mapInfoBean.getDisplayName().toLowerCase().contains(newValue.toLowerCase()));
      }
      if (!filteredMaps.isEmpty()) {
        mapListView.getSelectionModel().select(0);
      }
    });
    mapSearchTextField.setOnKeyPressed(event -> {
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
    });

    gameTypeComboBox.setCellFactory(param -> gameTypeCell());
    gameTypeComboBox.setButtonCell(gameTypeCell());
  }

  @NotNull
  private ListCell<GameTypeBean> gameTypeCell() {
    return new ListCell<GameTypeBean>() {

      @Override
      protected void updateItem(GameTypeBean item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setText(item.getName());
        }
      }
    };
  }

  @PostConstruct
  void postConstruct() {
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      preferencesService.addUpdateListener(preferences -> {
        if (preferencesService.getPreferences().getForgedAlliance().getPath() != null) {
          init();
        }
      });
    } else {
      init();
    }
  }

  private void init() {
    initRankingSlider();
    initModList();
    initMapSelection();
    initGameTypeComboBox();
    selectLastMap();
    setLastGameTitle();
    titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameTitle(newValue);
      preferencesService.storeInBackground();
    });
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

  private void initModList() {
    modListView.setCellFactory(param -> modListCell());

    modService.getInstalledModsInBackground(new Callback<List<ModInfoBean>>() {
      @Override
      public void success(List<ModInfoBean> result) {
        modListView.setItems(FXCollections.observableList(result));
      }

      @Override
      public void error(Throwable e) {
        logger.warn("Could not load mod list", e);
      }
    });
  }

  private void initMapSelection() {
    ObservableList<MapInfoBean> localMaps = mapService.getLocalMaps();

    filteredMaps = new FilteredList<>(localMaps);

    mapListView.setItems(filteredMaps);
    mapListView.setCellFactory(mapListCellFactory());
    mapListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        mapNameLabel.setText("");
        return;
      }
      String mapName = newValue.getDisplayName();

      mapNameLabel.setText(mapName);
      mapImageView.setImage(mapService.loadLargePreview(mapName));
      preferencesService.getPreferences().setLastMap(mapName);
      preferencesService.storeInBackground();
    });
  }

  private void initGameTypeComboBox() {
    gameService.addOnGameTypeInfoListener(change -> {
      change.getValueAdded();

      gameTypeComboBox.getItems().add(change.getValueAdded());
      selectLastOrDefaultGameType();
    });
  }

  private void selectLastMap() {
    String lastMap = preferencesService.getPreferences().getLastMap();
    for (MapInfoBean mapInfoBean : mapListView.getItems()) {
      if (mapInfoBean.getDisplayName().equals(lastMap)) {
        mapListView.getSelectionModel().select(mapInfoBean);
        break;
      }
    }
  }

  private void setLastGameTitle() {
    titleTextField.setText(preferencesService.getPreferences().getLastGameTitle());
  }

  @NotNull
  private ListCell<ModInfoBean> modListCell() {
    return new ListCell<ModInfoBean>() {

      @Override
      protected void updateItem(ModInfoBean item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setText(item.getName());
        }
      }
    };
  }

  @NotNull
  private javafx.util.Callback<ListView<MapInfoBean>, ListCell<MapInfoBean>> mapListCellFactory() {
    return param -> new ListCell<MapInfoBean>() {
      @Override
      protected void updateItem(MapInfoBean item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setText(item.getDisplayName());
        }
      }
    };
  }

  private void selectLastOrDefaultGameType() {
    String lastGameMod = preferencesService.getPreferences().getLastGameType();
    if (lastGameMod == null) {
      lastGameMod = FeaturedMod.DEFAULT_MOD.getString();
    }

    for (GameTypeBean mod : gameTypeComboBox.getItems()) {
      if (Objects.equals(mod.getName(), lastGameMod)) {
        gameTypeComboBox.getSelectionModel().select(mod);
        break;
      }
    }
  }

  @FXML
  void onRandomMapButtonClicked() {
    int mapIndex = (int) (Math.random() * filteredMaps.size());
    mapListView.getSelectionModel().select(mapIndex);
    mapListView.scrollTo(mapIndex);
  }

  @FXML
  void onCreateButtonClicked() {
    if (StringUtils.isEmpty(titleTextField.getText())) {
      // TODO tell the user
      return;
    }

    NewGameInfo newGameInfo = new NewGameInfo(
        titleTextField.getText(),
        Strings.emptyToNull(passwordTextField.getText()),
        gameTypeComboBox.getSelectionModel().getSelectedItem().getName(),
        mapListView.getSelectionModel().getSelectedItem().getDisplayName(),
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
