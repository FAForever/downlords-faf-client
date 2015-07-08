package com.faforever.client.game;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
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
import java.util.regex.Pattern;

// TODO rename all Game* things to "Play" to be consistent with the menu
public class GamesController  {

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
  Label gameTitleLabel;

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

  private ObservableList<GameInfoBean> gameInfoBeans;

  private Popup createGamePopup;
  private Popup passwordPopup;
  private FilteredList<GameInfoBean> filteredItems;

  //TODO Implement into options menu
  private boolean tilePaneSelected = false;
  private boolean firstGeneratedPane = true;

  public GamesController() {
    gameInfoBeans = FXCollections.observableArrayList();
  }

  @PostConstruct
  void postConstruct() {
    passwordPopup = new Popup();
    passwordPopup.setAutoHide(true);
    passwordPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    passwordPopup.getContent().setAll(enterPasswordController.getRoot());

    createGamePopup = new Popup();
    createGamePopup.setAutoFix(false);
    createGamePopup.setAutoHide(true);
    createGamePopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    createGamePopup.getContent().setAll(createGameController.getRoot());

    enterPasswordController.setOnPasswordEnteredListener(this::joinGame);

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

    filteredItems = new FilteredList<>(gameService.getGameInfoBeans());
  }

  public void displayGameDetail(GameInfoBean gameInfoBean) {
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

  public void joinGame(GameInfoBean gameInfoBean, String password, double screenX, double screenY) {
    // FIXME check if game path is set

    if (gameInfoBean.getAccess() == GameAccess.PASSWORD && password == null) {
      enterPasswordController.setGameInfoBean(gameInfoBean);
      passwordPopup.show(gamesRoot.getScene().getWindow(), screenX, screenY);
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
      gamesTiledController.createTiledFlowPane(filteredItems);

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
      gameTableController.initializeGameTable(filteredItems);

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
