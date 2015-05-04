package com.faforever.client.games;

import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.ModInfoMessage;
import com.faforever.client.legacy.OnModInfoMessageListener;
import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.legacy.message.GameInfoMessage;
import com.faforever.client.legacy.message.GameState;
import com.faforever.client.legacy.message.OnGameInfoMessageListener;
import com.faforever.client.maps.MapService;
import com.faforever.client.util.Callback;
import javafx.application.Platform;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

public class GamesController implements OnGameInfoMessageListener, OnModInfoMessageListener, CreateGameDialogController.OnCreateGameListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String DEFAULT_MOD = "faf";

  private static final Pattern RATING_PATTERN = Pattern.compile("([<>+~](?:\\d\\.?\\d?k|\\d{3,4})|(?:\\d\\.?\\d?k|\\d{3,4})[<>+]|(?:\\d\\.?\\d?k|\\d{1,4})\\s?-\\s?(?:\\d\\.?\\d?k|\\d{3,4}))");

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
  Button joinGameButton;

  @FXML
  TableView<GameInfoBean> gamesTable;

  @FXML
  TableColumn<GameInfoBean, Image> mapPreviewColumn;

  @FXML
  TableColumn<GameInfoBean, String> gameTitleColumn;

  @FXML
  TableColumn<GameInfoBean, String> playersColumn;

  @FXML
  TableColumn<GameInfoBean, Label> mapNameColumn;

  @FXML
  TableColumn<GameInfoBean, String> rankingColumn;

  @FXML
  TableColumn<GameInfoBean, GameState> gameStatusColumn;

  @FXML
  Button createGameButton;

  @Autowired
  GameService gameService;

  @Autowired
  ServerAccessor serverAccessor;

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
  void initialize() {
    initializeGameTable();
  }


  @PostConstruct
  void init() {
    gameService.addOnGameInfoListener(this);
    serverAccessor.addOnModInfoMessageListener(this);
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
    gameStatusColumn.setCellValueFactory(param -> param.getValue().statusProperty());

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
    GameInfoBean gameInfoBean = gamesTable.getSelectionModel().getSelectedItem();
    gameService.joinGame(gameInfoBean, new Callback<Void>() {
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
  public void onGameInfoMessage(GameInfoMessage gameInfoMessage) {
    Platform.runLater(() -> {
      if (!GameState.OPEN.equals(gameInfoMessage.state)) {
        gameInfoBeans.remove(gameInfoMessage.uid);
        return;
      }

      if (!gameInfoBeans.containsKey(gameInfoMessage.uid)) {
        gameInfoBeans.put(gameInfoMessage.uid, new GameInfoBean(gameInfoMessage));
      } else {
        gameInfoBeans.get(gameInfoMessage.uid).updateFromGameInfo(gameInfoMessage);
      }
    });
  }

  @Override
  public void onModInfoMessage(ModInfoMessage modInfoMessage) {
    if (!modInfoMessage.host || !modInfoMessage.live || modInfoBeans.containsKey(modInfoMessage.name)) {
      return;
    }

    modInfoBeans.put(modInfoMessage.name, new ModInfoBean(modInfoMessage));
  }

  public void configure(Stage stage) {
    this.stage = stage;
  }
}
