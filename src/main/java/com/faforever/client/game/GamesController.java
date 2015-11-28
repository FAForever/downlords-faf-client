package com.faforever.client.game;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.WindowDecorator;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.map.MapDetailController;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.OnChoseGameDirectoryListener;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.RatingUtil;
import javafx.application.Platform;
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
import javafx.scene.control.MenuButton;
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
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class GamesController {

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

  @Resource
  ApplicationContext applicationContext;
  @Resource
  I18n i18n;
  @Resource
  PlayerService playerService;
  @Resource
  GameService gameService;
  @Resource
  MapService mapService;
  @Resource
  CreateGameController createGameController;
  @Resource
  EnterPasswordController enterPasswordController;
  @Resource
  PreferencesService preferencesService;
  @Resource
  SceneFactory sceneFactory;
  @Resource
  NotificationService notificationService;
  @Resource
  OnChoseGameDirectoryListener onChoseGameDirectoryListener;

  private Popup createGamePopup;
  private Popup passwordPopup;
  private FilteredList<GameInfoBean> filteredItems;
  private Stage mapDetailPopup;

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

    ObservableList<GameInfoBean> gameInfoBeans = gameService.getGameInfoBeans();

    filteredItems = new FilteredList<>(gameInfoBeans);
    filteredItems.setPredicate(OPEN_GAMES_PREDICATE);

    if (preferencesService.getPreferences().getTilePaneSelected()) {
      onTilesButtonPressed();
    } else {
      onTableButtonPressed();
    }
  }

  @FXML
  void onTilesButtonPressed() {
    Preferences preferences = preferencesService.getPreferences();
    if (!preferences.getTilePaneSelected() || isFirstGeneratedPane()) {
      GamesTilesContainerController gamesTilesContainerController = applicationContext.getBean(GamesTilesContainerController.class);
      gamesTilesContainerController.createTiledFlowPane(filteredItems);

      Node root = gamesTilesContainerController.getRoot();
      populateContainer(root);

      preferences.setTilePaneSelected(true);
      firstGeneratedPane = false;
      preferencesService.storeInBackground();
    }
  }

  @FXML
  void onTableButtonPressed() {
    Preferences preferences = preferencesService.getPreferences();
    if (preferences.getTilePaneSelected() || isFirstGeneratedPane()) {
      GamesTableController gamesTableController = applicationContext.getBean(GamesTableController.class);
      Platform.runLater(() -> {
        gamesTableController.initializeGameTable(filteredItems);

        Node root = gamesTableController.getRoot();
        populateContainer(root);

        preferences.setTilePaneSelected(false);
        firstGeneratedPane = false;
        preferencesService.storeInBackground();
      });
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
      Platform.runLater(() -> {
        gameTitleLabel.setText(newValue);
        mapImageView.setImage(mapService.loadLargePreview(newValue));
      });
    });

    numberOfPlayersLabel.setText(i18n.get("game.detail.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()));
    hostLabel.textProperty().bind(gameInfoBean.hostProperty());
    mapLabel.textProperty().bind(gameInfoBean.mapTechnicalNameProperty());

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
      boolean teamCardSuccess = teamCardController.setTeam(entry.getValue(), entry.getKey());
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
              gameInfoBean -> !gameInfoBean.getPasswordProtected())
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
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      onChoseGameDirectoryListener.onChoseGameDirectory();
      return;
    }

    if (gameInfoBean.getPasswordProtected() && password == null) {
      enterPasswordController.setGameInfoBean(gameInfoBean);
      passwordPopup.show(gamesRoot.getScene().getWindow(), screenX, screenY);
    } else {
      gameService.joinGame(gameInfoBean, password);
    }
  }

  @FXML
  void onCreateGameButtonClicked(ActionEvent actionEvent) {
    Button button = (Button) actionEvent.getSource();

    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      onChoseGameDirectoryListener.onChoseGameDirectory();
      return;
    }

    Bounds screenBounds = createGameButton.localToScreen(createGameButton.getBoundsInLocal());
    createGamePopup.show(button.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }

  @FXML
  void onMapLargePreview() {
    if (currentGameInfoBean == null) {
      return;
    }
    mapDetailPopup = getMapDetailPopup();
    MapDetailController mapDetailController = applicationContext.getBean(MapDetailController.class);
    MapInfoBean mapInfoBean = mapService.getMapInfoBeanFromVaultByName(currentGameInfoBean.getMapTechnicalName());
    if (mapInfoBean == null) {
      mapDetailPopup.hide();
      String title = i18n.get("errorTitle");
      String message = i18n.get("mapPreview.loadFailure.message");
      notificationService.addNotification(new ImmediateNotification(title, message, Severity.WARN));
    } else {
      mapDetailController.createPreview(mapInfoBean);
      sceneFactory.createScene(mapDetailPopup, mapDetailController.getRoot(), false, WindowDecorator.WindowButtonType.CLOSE);
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
