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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO rename all Game* things to "Play" to be consistent with the menu
public class GamesController implements OnGameInfoListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Pattern RATING_PATTERN = Pattern.compile("([<>+~](?:\\d\\.?\\d?k|\\d{3,4})|(?:\\d\\.?\\d?k|\\d{3,4})[<>+]|(?:\\d\\.?\\d?k|\\d{1,4})\\s?-\\s?(?:\\d\\.?\\d?k|\\d{3,4}))");

  @FXML
  Label gameModeDescriptionLabel;

  @FXML
  Label mapLabel;

  @FXML
  Label mapDescriptionLabel;

  @FXML
  Button createGameButton;

  @FXML
  Pane gameViewContainer;

  @FXML
  Node gamesRoot;

  @FXML
  ImageView mapImageView;

  @FXML
  TextFlow gameTitleLabel;

  @FXML
  Label numberOfPlayersLabel;

  @FXML
  Label hosterLabel;

  @FXML
  Label gameModeLabel;

  @FXML
  VBox gamePreviewPanel;

  @Autowired
  ApplicationContext applicationContext;

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
  private FilteredList<GameInfoBean> filteredItems;
  private boolean tilePaneSelected = false;
  private boolean firstGeneratedPane = true;

  public GamesController() {
    gameInfoBeans = FXCollections.observableHashMap();
  }

  //TODO ask downloard does initialize get called before the object constructor
  @FXML
  void initialize() {
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

  //FIXME Text needs to wrap on long game titles
  public void displayGameDetail(GameInfoBean gameInfoBean) {
    mapImageView.setImage(mapService.loadLargePreview(gameInfoBean.getMapName()));
    Text gameTitle = new Text(gameInfoBean.getTitle());
    gameTitle.setWrappingWidth(mapImageView.getFitWidth());
    gameTitle.setFill(Paint.valueOf("#ffffff"));
    gameTitleLabel.getChildren().setAll(gameTitle);
    numberOfPlayersLabel.setText(i18n.get("game.detail.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()));
    hosterLabel.setText(gameInfoBean.getHost());
    gameModeLabel.setText(gameInfoBean.getFeaturedMod());
   // gameModeDescriptionLabel.setText(mapService.getMapInfoBeanFromString(gameInfoBean.getMapName()).getDescription());
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

  public void setFilteredList(FilteredList<GameInfoBean> filteredItems) {
    this.filteredItems = filteredItems;
  }

  public FilteredList<GameInfoBean> returnFilteredList(){
    return filteredItems;
  }

  public void joinSelectedGame(String password, GameInfoBean gameInfoBean, MouseEvent event) {
    // FIXME check if game path is set
    if (gameInfoBean.getAccess() == GameAccess.PASSWORD && password == null) {
      double lastMouseX = event.getScreenX();
      double lastMouseY = event.getScreenY();
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

  public boolean isFirstGeneratedPane() {
    return firstGeneratedPane;
  }

  @FXML
  void onTilesButtonPressed() {
    if (!tilePaneSelected || isFirstGeneratedPane()) {
      GamesTiledController gamesTiledController = applicationContext.getBean(GamesTiledController.class);
      gamesTiledController.createTiledFlowPane(gameInfoBeans);

      Node root = gamesTiledController.getRoot();
      populateContainer(root);
      firstGeneratedPane = false;
      tilePaneSelected = true;
    }
  }

  @FXML
  void onDetailsButtonPressed() {
    if (tilePaneSelected || isFirstGeneratedPane()) {
      GameTableController gameTableController = applicationContext.getBean(GameTableController.class);
      gameTableController.initializeGameTable(gameInfoBeans);

      Node root = gameTableController.getRoot();
      populateContainer(root);
      firstGeneratedPane = false;
      tilePaneSelected = false;
    }
  }

  private void populateContainer(Node root) {
    gameViewContainer.getChildren().setAll(root);
    AnchorPane.setBottomAnchor(root, 0d);
    AnchorPane.setLeftAnchor(root, 0d);
    AnchorPane.setRightAnchor(root, 0d);
    AnchorPane.setTopAnchor(root, 0d);
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


  public void onCreateGameButtonClicked(ActionEvent actionEvent) {
    Button button = (Button) actionEvent.getSource();

    Bounds screenBounds = createGameButton.localToScreen(createGameButton.getBoundsInLocal());
    createGamePopup.show(button.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }
}
