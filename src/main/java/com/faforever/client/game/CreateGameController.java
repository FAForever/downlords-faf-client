package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.JavaFxUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateGameController {

  public static final int MAX_RATING_LENGTH = 4;

  @FXML
  Label mapSizeLabel;
  @FXML
  Label mapPlayersLabel;
  @FXML
  Label mapAiMarkersLabel;
  @FXML
  Label mapDescriptionLabel;
  @FXML
  Label mapNameLabel;
  @FXML
  TextField mapSearchTextField;
  @FXML
  ImageView mapImageView;
  @FXML
  TextField titleTextField;
  @FXML
  ListView<ModInfoBean> modListView;
  @FXML
  TextField passwordTextField;
  @FXML
  TextField minRankingTextField;
  @FXML
  TextField maxRankingTextField;
  @FXML
  ListView<GameTypeBean> gameTypeListView;
  @FXML
  ListView<MapInfoBean> mapListView;
  @FXML
  Node createGameRoot;
  @FXML
  Button createGameButton;

  @Resource
  Environment environment;
  @Resource
  MapService mapService;
  @Resource
  ModService modService;
  @Resource
  GameService gameService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  I18n i18n;
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

    gameTypeListView.setCellFactory(param -> gameTypeCell());

    JavaFxUtil.makeNumericTextField(minRankingTextField, MAX_RATING_LENGTH);
    JavaFxUtil.makeNumericTextField(maxRankingTextField, MAX_RATING_LENGTH);
  }

  @NotNull
  private ListCell<GameTypeBean> gameTypeCell() {
    return new ListCell<GameTypeBean>() {

      @Override
      protected void updateItem(GameTypeBean item, boolean empty) {
        super.updateItem(item, empty);

        Platform.runLater(() -> {
          if (empty || item == null) {
            setText(null);
            setGraphic(null);
          } else {
            setText(item.getFullName());
          }
        });
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
    initModList();
    initMapSelection();
    initGameTypeComboBox();
    initRatingBoundaries();
    selectLastMap();
    setLastGameTitle();
    titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameTitle(newValue);
      preferencesService.storeInBackground();
    });
    createGameButton.disableProperty().bind(titleTextField.textProperty().isEmpty());
  }

  private void initModList() {
    modListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    modListView.setCellFactory(modListCellFactory());
    modListView.setItems(modService.getInstalledMods());
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

      preferencesService.getPreferences().setLastMap(newValue.getTechnicalName());
      preferencesService.storeInBackground();

      String mapDisplayName = newValue.getDisplayName();
      mapNameLabel.setText(newValue.getDisplayName());
      mapImageView.setImage(mapService.loadLargePreview(mapDisplayName));
      mapSizeLabel.setText(i18n.get("mapPreview.size", newValue.getSize()));
      mapPlayersLabel.setText(i18n.get("mapPreview.maxPlayers", newValue.getPlayers()));
      mapDescriptionLabel.setText(newValue.getDescription());

      if (newValue.getHasAiMarkers()) {
        mapAiMarkersLabel.setText(i18n.get("yes"));
      } else {
        mapAiMarkersLabel.setText(i18n.get("no"));
      }
    });
  }

  private void initGameTypeComboBox() {
    gameService.addOnGameTypeInfoListener(change -> {
      gameTypeListView.getItems().add(change.getValueAdded());
      selectLastOrDefaultGameType();
    });

    gameTypeListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameType(newValue.getName());
      preferencesService.storeInBackground();
    });
  }

  private void initRatingBoundaries() {
    int lastGameMinRating = preferencesService.getPreferences().getLastGameMinRating();
    int lastGameMaxRating = preferencesService.getPreferences().getLastGameMaxRating();

    minRankingTextField.setText(String.valueOf(lastGameMinRating));
    maxRankingTextField.setText(String.valueOf(lastGameMaxRating));

    minRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameMinRating(Integer.parseInt(newValue));
      preferencesService.storeInBackground();
    });
    maxRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameMaxRating(Integer.parseInt(newValue));
      preferencesService.storeInBackground();
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
  private Callback<ListView<ModInfoBean>, ListCell<ModInfoBean>> modListCellFactory() {
    return param -> {
      ListCell<ModInfoBean> cell = new ListCell<ModInfoBean>() {

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
      cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
        modListView.requestFocus();
        MultipleSelectionModel<ModInfoBean> selectionModel = modListView.getSelectionModel();
        if (!cell.isEmpty()) {
          int index = cell.getIndex();
          if (selectionModel.getSelectedIndices().contains(index)) {
            selectionModel.clearSelection(index);
          } else {
            selectionModel.select(index);
          }
          event.consume();
        }
      });
      return cell;
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
      lastGameMod = GameType.DEFAULT.getString();
    }

    for (GameTypeBean mod : gameTypeListView.getItems()) {
      if (Objects.equals(mod.getName(), lastGameMod)) {
        gameTypeListView.getSelectionModel().select(mod);
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
    ObservableList<ModInfoBean> selectedMods = modListView.getSelectionModel().getSelectedItems();

    Set<String> simMods = selectedMods.stream()
        .map(ModInfoBean::getId)
        .collect(Collectors.toSet());

    NewGameInfo newGameInfo = new NewGameInfo(
        titleTextField.getText(),
        Strings.emptyToNull(passwordTextField.getText()),
        gameTypeListView.getSelectionModel().getSelectedItem().getName(),
        mapListView.getSelectionModel().getSelectedItem().getDisplayName(),
        null,
        simMods);

    gameService.hostGame(newGameInfo);
  }

  public Node getRoot() {
    return createGameRoot;
  }

  @FXML
  void onSelectDefaultGameTypeButtonClicked(ActionEvent event) {
    for (GameTypeBean gameTypeBean : gameTypeListView.getItems()) {
      if (GameType.FAF.getString().equals(gameTypeBean.getName())) {
        gameTypeListView.getSelectionModel().select(gameTypeBean);
        return;
      }
    }
  }

  @FXML
  void onDeselectModsButtonClicked(ActionEvent event) {
    modListView.getSelectionModel().clearSelection();
  }

  @FXML
  void onReloadModsButtonClicked(ActionEvent event) {
    modService.loadInstalledMods();
  }
}
