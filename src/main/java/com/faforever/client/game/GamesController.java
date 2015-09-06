package com.faforever.client.game;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.WindowDecorator;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.map.MapPreviewLargeController;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.RatingUtil;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// TODO rename all Game* things to "Play" to be consistent with the menu
public class GamesController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Predicate<GameInfoBean> OPEN_GAMES_PREDICATE = gameInfoBean -> gameInfoBean.getStatus() == GameState.OPEN;

  @FXML
  VBox teamListPane;

  @FXML
  Label mapLabel;

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
  Label hostLabel;

  @FXML
  Label gameTypeLabel;

  @FXML
  VBox gamePreviewPanel;

  @FXML
  MenuButton switchViewButton;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  I18n i18n;

  @Autowired
  PlayerService playerService;

  @Autowired
  GameService gameService;

  @Autowired
  MapService mapService;

  @Autowired
  CreateGameController createGameController;

  @Autowired
  EnterPasswordController enterPasswordController;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  NotificationService notificationService;

  private Popup createGamePopup;
  private Popup passwordPopup;
  private FilteredList<GameInfoBean> filteredItems;
  private Stage mapDetailPopup;

  //TODO Implement into options menu
  private boolean tilesPaneSelected = false;
  private boolean firstGeneratedPane = true;
  private GameInfoBean currentGameInfoBean;


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

    enterPasswordController.setOnPasswordEnteredListener(this::doJoinGame);

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

    ObservableList<GameInfoBean> gameInfoBeans = gameService.getGameInfoBeans();

    filteredItems = new FilteredList<>(gameInfoBeans);
    filteredItems.setPredicate(OPEN_GAMES_PREDICATE);

    onTableButtonPressed();
  }

  @FXML
  void onTableButtonPressed() {
    if (tilesPaneSelected || isFirstGeneratedPane()) {
      GamesTableController gamesTableController = applicationContext.getBean(GamesTableController.class);
      gamesTableController.initializeGameTable(filteredItems);

      Node root = gamesTableController.getRoot();
      populateContainer(root);
      firstGeneratedPane = false;
      tilesPaneSelected = false;
    }
  }

  public boolean isFirstGeneratedPane() {
    return firstGeneratedPane;
  }

  private void populateContainer(Node root) {
    gameViewContainer.getChildren().setAll(root);
    AnchorPane.setBottomAnchor(root, 0d);
    AnchorPane.setLeftAnchor(root, 0d);
    AnchorPane.setRightAnchor(root, 0d);
    AnchorPane.setTopAnchor(root, 0d);
  }

  public void displayGameDetail(GameInfoBean gameInfoBean) {
    currentGameInfoBean = gameInfoBean;
    gameTitleLabel.setText(gameInfoBean.getTitle());
    mapImageView.setImage(mapService.loadLargePreview(gameInfoBean.getMapTechnicalName()));

    gameInfoBean.mapTechnicalNameProperty().addListener((observable, oldValue, newValue) -> {
      gameTitleLabel.setText(newValue);
      mapImageView.setImage(mapService.loadLargePreview(newValue));
    });

    numberOfPlayersLabel.setText(i18n.get("game.detail.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()));
    hostLabel.textProperty().bind(gameInfoBean.hostProperty());
    mapLabel.textProperty().bind(gameInfoBean.technicalNameProperty());

    gameInfoBean.featuredModProperty().addListener((observable, oldValue, newValue) -> {
      updateGameType(newValue);
    });
    updateGameType(gameInfoBean.getFeaturedMod());


    createTeams(gameInfoBean.getTeams());
  }

  private void updateGameType(String newValue) {
    GameTypeBean gameType = gameService.getGameTypeByString(newValue);
    String fullName = gameType != null ? gameType.getFullName() : null;
    gameTypeLabel.setText(StringUtils.defaultString(fullName));
  }

  private void createTeams(ObservableMap<? extends String, ? extends List<String>> playersByTeamNumber) {
    teamListPane.getChildren().clear();
    for (Map.Entry<? extends String, ? extends List<String>> entry : playersByTeamNumber.entrySet()) {
      TeamCardController teamCardController = applicationContext.getBean(TeamCardController.class);
      boolean teamCardSuccess = teamCardController.setTeam(entry.getValue(), Integer.parseInt(entry.getKey()));
      if (teamCardSuccess) {
        teamListPane.getChildren().add(teamCardController.getRoot());
      }
    }
  }

  @FXML
  void onShowPrivateGames(ActionEvent actionEvent) {
    CheckBox checkBox = (CheckBox) actionEvent.getSource();
    boolean selected = checkBox.isSelected();
    if (selected) {
      filteredItems.setPredicate(OPEN_GAMES_PREDICATE);
    } else {
      filteredItems.setPredicate(
          OPEN_GAMES_PREDICATE.and(
              gameInfoBean -> gameInfoBean.getAccess() != GameAccess.PASSWORD)
      );
    }
  }

  public void onJoinGame(GameInfoBean gameInfoBean, String password, double screenX, double screenY) {
    PlayerInfoBean currentPlayer = playerService.getCurrentPlayer();
    int playerRating = RatingUtil.getGlobalRating(currentPlayer);

    if ((playerRating < gameInfoBean.getMinRating() || playerRating > gameInfoBean.getMaxRating())) {
      showRatingOutOfBoundsConfirmation(playerRating, gameInfoBean, screenX, screenY);
    } else {
      doJoinGame(gameInfoBean, password, screenX, screenY);
    }
  }

  private void showRatingOutOfBoundsConfirmation(int playerRating, GameInfoBean gameInfoBean, double screenX, double screenY) {
    notificationService.addNotification(new ImmediateNotification(
        i18n.get("game.joinGameRatingConfirmation.title"),
        i18n.get("game.joinGameRatingConfirmation.text", gameInfoBean.getMinRating(), gameInfoBean.getMaxRating(), playerRating),
        Severity.INFO,
        Arrays.asList(
            new Action(i18n.get("game.join"), event -> doJoinGame(gameInfoBean, null, screenX, screenY)),
            new Action(i18n.get("game.cancel"))
        )
    ));
  }

  private void doJoinGame(GameInfoBean gameInfoBean, String password, double screenX, double screenY) {
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

  @FXML
  void onTilesButtonPressed() {
    if (!tilesPaneSelected || isFirstGeneratedPane()) {
      GamesTilesContainerController gamesTilesContainerController = applicationContext.getBean(GamesTilesContainerController.class);
      gamesTilesContainerController.createTiledFlowPane(filteredItems);

      Node root = gamesTilesContainerController.getRoot();
      populateContainer(root);
      firstGeneratedPane = false;
      tilesPaneSelected = true;
    }
  }

  @FXML
  void onCreateGameButtonClicked(ActionEvent actionEvent) {
    Button button = (Button) actionEvent.getSource();

    Bounds screenBounds = createGameButton.localToScreen(createGameButton.getBoundsInLocal());
    createGamePopup.show(button.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }

  //TODO do we want to create new pane or repopulate the same pane
  @FXML
  void onMapLargePreview(Event event) {
    if (currentGameInfoBean == null) {
      return;
    }
    mapDetailPopup = getMapDetailPopup();
    MapPreviewLargeController mapPreviewLargeController = applicationContext.getBean(MapPreviewLargeController.class);
    MapInfoBean mapInfoBean = mapService.getMapInfoBeanFromVaultFromName(currentGameInfoBean.getMapTechnicalName());
    if (mapInfoBean == null) {
      mapDetailPopup.hide();
      String title = i18n.get("mapPreview.loadFailure.title");
      String message = i18n.get("mapPreview.loadFailure.message");
      notificationService.addNotification(new ImmediateNotification(title, message, Severity.ERROR));
    } else {
      mapPreviewLargeController.createPreview(mapInfoBean);
      sceneFactory.createScene(mapDetailPopup, mapPreviewLargeController.getRoot(), false, WindowDecorator.WindowButtonType.CLOSE);
      mapDetailPopup.centerOnScreen();
      mapDetailPopup.show();
    }
  }

  private Stage getMapDetailPopup() {
    if (mapDetailPopup == null) {
      mapDetailPopup = new Stage(StageStyle.TRANSPARENT);
      mapDetailPopup.initModality(Modality.NONE);
      mapDetailPopup.initOwner(getRoot().getScene().getWindow());
    }
    return mapDetailPopup;
  }

  public Node getRoot() {
    return gamesRoot;
  }
}
