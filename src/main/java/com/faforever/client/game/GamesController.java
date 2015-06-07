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
import com.faforever.client.util.Callback;
import javafx.application.Platform;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;

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
  SplitMenuButton createGameButton;

  @Autowired
  GameService gameService;

  @Autowired
  ModService modService;

  @Autowired
  I18n i18n;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  CreateGameDialogFactory createGameDialogFactory;

  @Autowired
  MapService mapService;

  @Autowired
  FxmlLoader fxmlLoader;

  private ObservableMap<Integer, GameInfoBean> gameInfoBeans;

  private Map<String, ModInfoBean> modInfoBeans;

  private Stage stage;

  private Stage createGameDialogStage;

  public GamesController() {
    gameInfoBeans = FXCollections.observableHashMap();
    modInfoBeans = new HashMap<>();
  }

  @FXML
  void setUpIfNecessary() {
    initializeGameTable();
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

  public void onCreateGameButtonClicked(ActionEvent actionEvent) {
    if (createGameDialogStage != null) {
      return;
    }

    CreateGameDialogController controller = createGameDialogFactory.create();
    controller.setOnGameCreateListener(this);
    controller.setMods(modInfoBeans.values());
    controller.setMaps(mapService.getLocalMaps());

    createGameDialogStage = new Stage(StageStyle.TRANSPARENT);
    createGameDialogStage.initModality(Modality.NONE);
    createGameDialogStage.initOwner(this.stage);

    sceneFactory.createScene(createGameDialogStage, controller.getRoot(), false, CLOSE);

    createGameDialogStage.setOnShowing(event -> {
      createGameButton.setDisable(true);
    });
    createGameDialogStage.setOnHiding(event -> {
      createGameDialogStage = null;
      createGameButton.setDisable(false);
    });

    createGameDialogStage.show();
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
}
