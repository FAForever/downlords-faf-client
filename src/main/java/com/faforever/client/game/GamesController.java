package com.faforever.client.game;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO rename all Game* things to "Play" to be consistent with the menu
public class GamesController implements OnGameInfoListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Pattern RATING_PATTERN = Pattern.compile("([<>+~](?:\\d\\.?\\d?k|\\d{3,4})|(?:\\d\\.?\\d?k|\\d{3,4})[<>+]|(?:\\d\\.?\\d?k|\\d{1,4})\\s?-\\s?(?:\\d\\.?\\d?k|\\d{3,4}))");

  @FXML
  Button createGameButton;

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
  VBox gamePreviewPanel;

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
  PreferencesService preferenceService;

  @Autowired
  I18n i18n;

  @Autowired
  GameService gameService;

  @Autowired
  ModService modService;

  @Autowired
  MapService mapService;

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  CreateGameController createGameController;

  @Autowired
  EnterPasswordController enterPasswordController;

  @Autowired
  PreferencesService preferencesService;

  private ObservableMap<Integer, GameInfoBean> gameInfoBeans;

  private Popup createGamePopup;
  private Popup passwordPopup;
  private double lastMouseX;
  private double lastMouseY;
  private FilteredList<GameInfoBean> filteredItems;

  public GamesController() {
    gameInfoBeans = FXCollections.observableHashMap();
  }

  @FXML
  void initialize() {
    initializeGameTable();
  }

  @PostConstruct
  void postConstruct() {
    gameService.addOnGameInfoListener(this);

    passwordPopup = new Popup();
    passwordPopup.setAutoHide(true);
    passwordPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    passwordPopup.getContent().setAll(enterPasswordController.getRoot());

    createGamePopup = new Popup();
    createGamePopup.setAutoFix(false);
    createGamePopup.setAutoHide(true);
    createGamePopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    createGamePopup.getContent().setAll(createGameController.getRoot());

    enterPasswordController.setOnPasswordEnteredListener(this::joinSelectedGame);

    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      createGameButton.setDisable(true);
      createGameButton.setTooltip(new Tooltip(i18n.get("missingGamePath.notification")));

      preferencesService.addUpdateListener(preferences -> {
        if (preferencesService.getPreferences().getForgedAlliance().getPath() != null) {
          createGameButton.setDisable(false);
          createGameButton.setTooltip(null);
        }
      });
    }
  }

  private void initializeGameTable() {
    ObservableList<GameInfoBean> tableItems = FXCollections.observableArrayList();
    filteredItems = new FilteredList<>(tableItems);
    gamesTable.setItems(filteredItems);

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
        return Strings.nullToEmpty(extractRating(param.getValue().getTitle()));
      }
    });
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());

    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null && !filteredItems.isEmpty()) {
        gamesTable.getSelectionModel().select(filteredItems.get(0));
      } else {
        displayGameDetail(newValue);
      }
    });

    accessColumn.setCellValueFactory(param -> param.getValue().accessProperty());
  }

  private void displayGameDetail(GameInfoBean gameInfoBean) {
    mapImageView.setImage(mapService.loadLargePreview(gameInfoBean.getMapName()));
    gameTitleLabel.setText(gameInfoBean.getTitle());
    numberOfPlayersLabel.setText(i18n.get("game.detail.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()));
    hosterLabel.setText(gameInfoBean.getHost());
    gameModeLabel.setText(gameInfoBean.getFeaturedMod());
  }

  @FXML
  void onShowPrivateGames(ActionEvent actionEvent) {
    CheckBox checkBox = (CheckBox) actionEvent.getSource();
    boolean selected = checkBox.isSelected();

    if (selected) {
      filteredItems.setPredicate(gameInfoBean -> true);
    } else {
      filteredItems.setPredicate(gameInfoBean -> gameInfoBean.getAccess() != GameAccess.PASSWORD);
    }
  }

  private void joinSelectedGame(String password) {
    // FIXME check if game path is set
    GameInfoBean gameInfoBean = gamesTable.getSelectionModel().getSelectedItem();

    if (gameInfoBean.getAccess() == GameAccess.PASSWORD && password == null) {
      passwordPopup.show(gamesRoot.getScene().getWindow(), lastMouseX, lastMouseY);
    } else {
      gameService.joinGame(gameInfoBean, password, new Callback<Void>() {
        @Override
        public void success(Void result) {
          // Cool.
        }

        @Override
        public void error(Throwable e) {
          // FIXME implement
          logger.warn("Game could not be joined", e);
        }
      });
    }
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

  public void setUpIfNecessary() {
  }

  public Node getRoot() {
    return gamesRoot;
  }

  public void onTableClicked(MouseEvent event) {
    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
      lastMouseX = event.getScreenX();
      lastMouseY = event.getScreenY();
      joinSelectedGame(null);
    }
  }

  public void onCreateGameButtonClicked(ActionEvent actionEvent) {
    Button button = (Button) actionEvent.getSource();

    Bounds screenBounds = createGameButton.localToScreen(createGameButton.getBoundsInLocal());
    createGamePopup.show(button.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }
}
