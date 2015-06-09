package com.faforever.client.game;

import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.OnModInfoListener;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.ModInfo;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import impl.org.controlsfx.skin.CheckComboBoxSkin;
import javafx.application.Platform;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Skin;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.RangeSlider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GamesController implements OnGameInfoListener, OnModInfoListener, CreateGameDialogController.OnCreateGameListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String DEFAULT_MOD = "faf";

  private static final Pattern RATING_PATTERN = Pattern.compile("([<>+~](?:\\d\\.?\\d?k|\\d{3,4})|(?:\\d\\.?\\d?k|\\d{3,4})[<>+]|(?:\\d\\.?\\d?k|\\d{1,4})\\s?-\\s?(?:\\d\\.?\\d?k|\\d{3,4}))");

  @FXML
  Node gamesRoot;

  @FXML
  ImageView mapImageView;

  @FXML
  Label gameTitleLabel;

  @FXML
  Label numberOfPlayersLabel;

  @FXML
  Label hosterLabel;

  @FXML
  Label gameModeLabel;

  @FXML
  VBox createGamePanel;

  @FXML
  VBox gamePreviewPanel;

  @FXML
  TextField titleTextField;

  @FXML
  TextField passwordTextField;

  @FXML
  ComboBox<MapInfoBean> mapComboBox;

  @FXML
  CheckComboBox<ModInfoBean> additionalModsCheckComboBox;

  @FXML
  TextField minRankingTextField;

  @FXML
  RangeSlider rankingSlider;

  @FXML
  TextField maxRankingTextField;

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
  SplitMenuButton showCreateGamePanelButton;

  @Autowired
  PreferencesService preferenceService;

  public interface OnCreateGameListener {

    void onCreateGame(NewGameInfo newGameInfo);
  }

  @Autowired
  I18n i18n;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  CreateGameDialogFactory createGameDialogFactory;

  @Autowired
  GameService gameService;

  @Autowired
  ModService modService;

  @Autowired
  MapService mapService;

  @Autowired
  FxmlLoader fxmlLoader;

  private ObservableMap<Integer, GameInfoBean> gameInfoBeans;

  private Map<String, ModInfoBean> modInfoBeans;

  private Stage stage;

  private Stage createGameDialogStage;

  private OnCreateGameListener onGameCreateListener;

  private ModInfoBean selectedMod;

  public GamesController() {
    gameInfoBeans = FXCollections.observableHashMap();
    modInfoBeans = new HashMap<>();
  }

  @FXML
  void initialize() {
    initializeGameTable();

    createGamePanel.managedProperty().bind(createGamePanel.visibleProperty());


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

      rankingSlider.setMax(2500);
      rankingSlider.setMin(0);
      rankingSlider.setHighValue(1300);
      rankingSlider.setLowValue(800);

      mapComboBox.setButtonCell(new ListCell<MapInfoBean>() {
          @Override
          protected void updateItem(MapInfoBean item, boolean empty) {
              super.updateItem(item, empty);
              if (item == null || empty) {
                  return;
              }

              Image mapPreview = mapService.loadSmallPreview(item.getName());
              setGraphic(new ImageView(mapPreview));
              setText(item.getName());
          }
      });

      additionalModsCheckComboBox.setConverter(new StringConverter<ModInfoBean>() {
          @Override
          public String toString(ModInfoBean object) {
              return object.getFullName();
          }

          @Override
          public ModInfoBean fromString(String string) {
              return null;
          }
      });

    additionalModsCheckComboBox.skinProperty().addListener(new ChangeListener<Skin>() {
      @Override
      public void changed(ObservableValue<? extends Skin> observable, Skin oldValue, Skin newValue) {
        if(oldValue==null && newValue!=null){
          CheckComboBoxSkin skin = (CheckComboBoxSkin)newValue;
          ComboBox combo = (ComboBox)skin.getChildren().get(0);
          combo.setPrefWidth(300.0);
          combo.setMaxWidth(Double.MAX_VALUE);
        }
      }
    });
  }


  @PostConstruct
  void init() {
    gameService.addOnGameInfoListener(this);
    modService.addOnModInfoListener(this);
  }

  private void initializeGameTable() {
    gamesTable.setEditable(false);
    gameInfoBeans.addListener((MapChangeListener<Integer, GameInfoBean>) change -> {
      if (change.wasAdded()) {
        gamesTable.getItems().add(change.getValueAdded());
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
        return StringUtils.defaultString(extractRating(param.getValue().getTitle()));
      }
    });

    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      displayGameDetail(newValue);
    });
  }

  private void displayGameDetail(GameInfoBean gameInfoBean) {
    mapImageView.setImage(mapService.loadLargePreview(gameInfoBean.getMapName()));
    gameTitleLabel.setText(gameInfoBean.getTitle());
    numberOfPlayersLabel.setText(String.format(i18n.get("game.detail.players.format"), gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()));
    hosterLabel.setText(gameInfoBean.getHost());
    gameModeLabel.setText(gameInfoBean.getFeaturedMod());
  }

  @Override
  public void onCreateGame(NewGameInfo newGameInfo) {
    gameService.hostGame(newGameInfo, new Callback<Void>() {
      @Override
      public void success(Void result) {
        createGameDialogStage.close();
      }

      @Override
      public void error(Throwable e) {
        // FIXME implement
        logger.warn("Game could not be hosted", e);
      }
    });
  }

  public void onShowPrivateGames(ActionEvent actionEvent) {

  }

  @FXML
  void onJoinGameButtonClicked(ActionEvent event) {
    joinSelectedGame();
  }

  private void joinSelectedGame() {
    GameInfoBean gameInfoBean = gamesTable.getSelectionModel().getSelectedItem();
    if (gameInfoBean == null) {
      // TODO better to disable the button
      return;
    }

    // FIXME implement
    String password = null;
    gameService.joinGame(gameInfoBean, password, new Callback<Void>() {
      @Override
      public void success(Void result) {
        // cool.
      }

      @Override
      public void error(Throwable e) {
        // FIXME implement
        logger.warn("Game could not be hosted", e);
      }
    });
  }

  public void onShowCreateGamePanelButtonClicked(ActionEvent event) {

  }

  private static String extractRating(String title) {
    Matcher matcher = RATING_PATTERN.matcher(title);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {
    Platform.runLater(() -> {
      if (!GameState.OPEN.equals(gameInfo.state)) {
        gameInfoBeans.remove(gameInfo.uid);
        return;
      }

      if (!gameInfoBeans.containsKey(gameInfo.uid)) {
        gameInfoBeans.put(gameInfo.uid, new GameInfoBean(gameInfo));
      } else {
        gameInfoBeans.get(gameInfo.uid).updateFromGameInfo(gameInfo);
      }
    });
  }

  @Override
  public void onModInfo(ModInfo modInfo) {
    if (!modInfo.host || !modInfo.live || modInfoBeans.containsKey(modInfo.name)) {
      return;
    }

    modInfoBeans.put(modInfo.name, new ModInfoBean(modInfo));
  }

  public void setUpIfNecessary(Stage stage) {
    this.stage = stage;
  }

  public Node getRoot() {
    return gamesRoot;
  }

  public void onTableClicked(MouseEvent event) {
    if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
      joinSelectedGame();
    }
  }

  public void setMaps(ObservableList<MapInfoBean> maps) {
    mapComboBox.getItems().setAll(maps);
    mapComboBox.setCellFactory(param -> new MapListCell());

    String lastMap = preferenceService.getPreferences().getLastMap();
    for (MapInfoBean map : maps) {
      if (Objects.equals(map.getName(), lastMap)) {
        mapComboBox.getSelectionModel().select(map);
        break;
      }
    }
  }

  public void onCreateGameButtonClicked(ActionEvent actionEvent) {
    onGameCreateListener.onCreateGame(
            new NewGameInfo(
                    titleTextField.getText(),
                    passwordTextField.getText(),
                    selectedMod.getName(),
                    mapComboBox.getValue().getName(),
                0)
    );
  }

  public void setOnGameCreateListener(OnCreateGameListener listener) {
    onGameCreateListener = listener;
  }

}
