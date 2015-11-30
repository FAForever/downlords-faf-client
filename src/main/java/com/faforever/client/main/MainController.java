package com.faforever.client.main;

import com.faforever.client.cast.CastsController;
import com.faforever.client.chat.ChatController;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesController;
import com.faforever.client.gravatar.GravatarService;
import com.faforever.client.hub.CommunityHubController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardController;
import com.faforever.client.legacy.OnFafDisconnectedListener;
import com.faforever.client.legacy.OnLobbyConnectedListener;
import com.faforever.client.legacy.OnLobbyConnectingListener;
import com.faforever.client.lobby.LobbyService;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.news.NewsController;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.ImmediateNotificationController;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotificationsController;
import com.faforever.client.patch.GameUpdateService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.portcheck.PortCheckService;
import com.faforever.client.preferences.OnChoseGameDirectoryListener;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.SettingsController;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;
import com.faforever.client.rankedmatch.Ranked1v1Controller;
import com.faforever.client.rankedmatch.RankedMatchNotification;
import com.faforever.client.replay.ReplayVaultController;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.JavaFxUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MAXIMIZE_RESTORE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MINIMIZE;

public class MainController implements OnLobbyConnectedListener, OnLobbyConnectingListener, OnFafDisconnectedListener, OnChoseGameDirectoryListener, OnRankedMatchNotificationListener {

  private static final PseudoClass NOTIFICATION_INFO_PSEUDO_CLASS = PseudoClass.getPseudoClass("info");
  private static final PseudoClass NOTIFICATION_WARN_PSEUDO_CLASS = PseudoClass.getPseudoClass("warn");
  private static final PseudoClass NOTIFICATION_ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
  private static final PseudoClass NAVIGATION_ACTIVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("active");

  @FXML
  ImageView userImageView;
  @FXML
  HBox mainNavigation;
  @FXML
  Pane mainHeaderPane;
  @FXML
  ButtonBase notificationsButton;
  @FXML
  Pane contentPane;
  @FXML
  SplitMenuButton communityButton;
  @FXML
  SplitMenuButton chatButton;
  @FXML
  SplitMenuButton playButton;
  @FXML
  SplitMenuButton vaultButton;
  @FXML
  SplitMenuButton leaderboardButton;
  @FXML
  ProgressBar taskProgressBar;
  @FXML
  Pane mainRoot;
  @FXML
  Button usernameButton;
  @FXML
  Pane taskPane;
  @FXML
  Labeled portCheckStatusButton;
  @FXML
  MenuButton fafConnectionButton;
  @FXML
  MenuButton ircConnectionButton;
  @FXML
  Label taskProgressLabel;
  @FXML
  Pane rankedMatchNotificationPane;

  @Resource
  Environment environment;
  @Resource
  NewsController newsController;
  @Resource
  ChatController chatController;
  @Resource
  GamesController gamesController;
  @Resource
  Ranked1v1Controller ranked1v1Controller;
  @Resource
  LeaderboardController leaderboardController;
  @Resource
  ReplayVaultController replayVaultController;
  @Resource
  PersistentNotificationsController persistentNotificationsController;
  @Resource
  TransientNotificationsController transientNotificationsController;
  @Resource
  PreferencesService preferencesService;
  @Resource
  SceneFactory sceneFactory;
  @Resource
  LobbyService lobbyService;
  @Resource
  PortCheckService portCheckService;
  @Resource
  I18n i18n;
  @Resource
  UserService userService;
  @Resource
  GravatarService gravatarService;
  @Resource
  TaskService taskService;
  @Resource
  NotificationService notificationService;
  @Resource
  SettingsController settingsController;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  PlayerService playerService;
  @Resource
  ModVaultController modVaultController;
  @Resource
  MapVaultController mapMapVaultController;
  @Resource
  CastsController castsController;
  @Resource
  CommunityHubController communityHubController;
  @Resource
  GameUpdateService gameUpdateService;
  @Resource
  GameService gameService;
  @Resource
  ClientUpdateService clientUpdateService;
  @Resource
  UserMenuController userMenuController;
  @Resource
  Stage stage;
  @Resource
  Locale locale;

  @VisibleForTesting
  Popup persistentNotificationsPopup;
  private Popup userMenuPopup;
  private ChangeListener<Boolean> windowFocusListener;
  private Popup transientNotificationsPopup;


  @FXML
  void initialize() {
    taskPane.managedProperty().bind(taskPane.visibleProperty());
    taskProgressBar.managedProperty().bind(taskProgressBar.visibleProperty());
    taskProgressLabel.managedProperty().bind(taskProgressLabel.visibleProperty());
    rankedMatchNotificationPane.managedProperty().bind(rankedMatchNotificationPane.visibleProperty());

    rankedMatchNotificationPane.setVisible(false);

    addHoverListener(playButton);
    addHoverListener(communityButton);
    addHoverListener(chatButton);
    addHoverListener(vaultButton);
    addHoverListener(leaderboardButton);

    setCurrentTaskInStatusBar(null);

    windowFocusListener = (observable, oldValue, newValue) -> {
      if (!newValue) {
        hideAllMenuDropdowns();
      }
    };
  }

  private void addHoverListener(SplitMenuButton button) {
    button.hoverProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        showMenuDropdown(button);
      }
    });
  }

  /**
   * @param task the task to set, {@code null} to unset
   */
  private void setCurrentTaskInStatusBar(PrioritizedTask<?> task) {
    Platform.runLater(() -> {
      if (task == null) {
        taskProgressBar.setVisible(false);
        taskProgressLabel.setVisible(false);
        return;
      }

      taskProgressBar.setVisible(true);
      taskProgressBar.progressProperty().bind(task.progressProperty());

      taskProgressLabel.setVisible(true);
      taskProgressLabel.textProperty().bind(task.titleProperty());
    });
  }

  private void hideAllMenuDropdowns() {
    mainNavigation.getChildren().forEach(item -> ((SplitMenuButton) item).hide());
  }

  private void showMenuDropdown(SplitMenuButton button) {
    mainNavigation.getChildren().stream()
        .filter(item -> item instanceof SplitMenuButton && item != button)
        .forEach(item -> ((SplitMenuButton) item).hide());
    button.show();
  }

  @PostConstruct
  void postConstruct() {
    persistentNotificationsPopup = new Popup();
    persistentNotificationsPopup.getContent().setAll(persistentNotificationsController.getRoot());
    persistentNotificationsPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT);
    persistentNotificationsPopup.setAutoFix(false);
    persistentNotificationsPopup.setAutoHide(true);

    transientNotificationsPopup = new Popup();
    transientNotificationsPopup.getScene().getRoot().getStyleClass().add("transient-notification");
    transientNotificationsPopup.getContent().setAll(transientNotificationsController.getRoot());
    transientNotificationsPopup.anchorLocationProperty().bind(Bindings.createObjectBinding(() -> {
          switch (preferencesService.getPreferences().getNotification().getToastPosition()) {
            case TOP_RIGHT:
              return PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT;
            case BOTTOM_LEFT:
              return PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT;
            case TOP_LEFT:
              return PopupWindow.AnchorLocation.CONTENT_TOP_LEFT;
            default:
              return PopupWindow.AnchorLocation.CONTENT_BOTTOM_RIGHT;
          }
        }, preferencesService.getPreferences().getNotification().toastPositionProperty()
    ));
    transientNotificationsController.getRoot().getChildren().addListener((InvalidationListener) observable -> {
      if (!transientNotificationsController.getRoot().getChildren().isEmpty()) {
        Rectangle2D visualBounds = getTransientNotificationAreaBounds();
        transientNotificationsPopup.show(mainRoot.getScene().getWindow(), visualBounds.getMaxX(), visualBounds.getMaxY());
      } else {
        transientNotificationsPopup.hide();
      }
    });

    userMenuPopup = new Popup();
    userMenuPopup.setAutoFix(false);
    userMenuPopup.setAutoHide(true);
    userMenuPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT);
    userMenuPopup.getContent().setAll(userMenuController.getRoot());

    notificationService.addPersistentNotificationListener(
        change -> Platform.runLater(() -> updateNotificationsButton(change.getSet()))
    );
    notificationService.addImmediateNotificationListener(
        notification -> Platform.runLater(() -> displayImmediateNotification(notification))
    );
    notificationService.addTransientNotificationListener(
        notification -> Platform.runLater(() -> transientNotificationsController.addNotification(notification))
    );

    taskService.getActiveTasks().addListener((Observable observable) -> {
      Collection<PrioritizedTask<?>> runningTasks = taskService.getActiveTasks();
      if (runningTasks.isEmpty()) {
        setCurrentTaskInStatusBar(null);
      } else {
        setCurrentTaskInStatusBar(runningTasks.iterator().next());
      }
    });

    portCheckStatusButton.getTooltip().setText(
        i18n.get("statusBar.portCheckTooltip", preferencesService.getPreferences().getForgedAlliance().getPort())
    );

    preferencesService.setOnChoseGameDirectoryListener(this);
    gameService.addOnRankedMatchNotificationListener(this);
    userService.addOnLoginListener(this::onLoggedIn);
  }

  private Rectangle2D getTransientNotificationAreaBounds() {
    ObservableList<Screen> screens = Screen.getScreens();

    int toastScreenIndex = preferencesService.getPreferences().getNotification().getToastScreen();
    Screen screen;
    if (toastScreenIndex < screens.size()) {
      screen = screens.get(toastScreenIndex);
    } else {
      screen = Screen.getPrimary();
    }
    return screen.getVisualBounds();
  }

  /**
   * Updates the number displayed in the notifications button and sets its CSS pseudo class based on the highest
   * notification {@code Severity} of all current notifications.
   */
  private void updateNotificationsButton(Collection<? extends PersistentNotification> notifications) {
    JavaFxUtil.assertApplicationThread();

    int numberOfNotifications = notifications.size();
    notificationsButton.setText(String.format(locale, "%d", numberOfNotifications));

    Severity highestSeverity = null;
    for (PersistentNotification notification : notifications) {
      if (highestSeverity == null || notification.getSeverity().compareTo(highestSeverity) > 0) {
        highestSeverity = notification.getSeverity();
      }
    }

    notificationsButton.pseudoClassStateChanged(NOTIFICATION_INFO_PSEUDO_CLASS, highestSeverity == Severity.INFO);
    notificationsButton.pseudoClassStateChanged(NOTIFICATION_WARN_PSEUDO_CLASS, highestSeverity == Severity.WARN);
    notificationsButton.pseudoClassStateChanged(NOTIFICATION_ERROR_PSEUDO_CLASS, highestSeverity == Severity.ERROR);
  }

  private void displayImmediateNotification(ImmediateNotification notification) {
    ImmediateNotificationController controller = applicationContext.getBean(ImmediateNotificationController.class);
    controller.setNotification(notification);

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(mainRoot.getScene().getWindow());

    sceneFactory.createScene(userInfoWindow, controller.getRoot(), false, CLOSE);

    userInfoWindow.show();
  }

  private void onLoggedIn() {
    Platform.runLater(this::display);
  }

  public void display() {
    lobbyService.setOnFafConnectedListener(this);
    lobbyService.setOnLobbyConnectingListener(this);
    lobbyService.setOnFafDisconnectedListener(this);

    sceneFactory.createScene(stage, mainRoot, true, MINIMIZE, MAXIMIZE_RESTORE, CLOSE);

    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
    stage.setTitle(environment.getProperty("mainWindowTitle"));
    restoreState(mainWindowPrefs, stage);
    stage.show();

    registerWindowListeners(stage, mainWindowPrefs);

    usernameButton.setText(userService.getUsername());
    // TODO no more e-mail address :(
//    userImageView.setImage(gravatarService.getGravatar(userService.getEmail()));

    checkGamePortInBackground();
    gameUpdateService.checkForUpdateInBackground();
    clientUpdateService.checkForUpdateInBackground();
  }

  private void restoreState(WindowPrefs mainWindowPrefs, Stage stage) {
    String lastView = mainWindowPrefs.getLastView();
    if (lastView != null) {
      mainNavigation.getChildren().stream()
          .filter(button -> button instanceof ButtonBase)
          .filter(button -> lastView.equals(button.getId()))
          .forEach(button -> ((ButtonBase) button).fire());
    } else {
      communityButton.fire();
    }
  }

  private void registerWindowListeners(final Stage stage, final WindowPrefs mainWindowPrefs) {
    stage.maximizedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        stage.setWidth(mainWindowPrefs.getWidth());
        stage.setHeight(mainWindowPrefs.getHeight());
        stage.setX(mainWindowPrefs.getX());
        stage.setY(mainWindowPrefs.getY());
      }
      mainWindowPrefs.setMaximized(newValue);
      preferencesService.storeInBackground();
    });
    stage.heightProperty().addListener((observable, oldValue, newValue) -> {
      if (!stage.isMaximized()) {
        mainWindowPrefs.setHeight(newValue.intValue());
        preferencesService.storeInBackground();
      }
    });
    stage.widthProperty().addListener((observable, oldValue, newValue) -> {
      if (!stage.isMaximized()) {
        mainWindowPrefs.setWidth(newValue.intValue());
        preferencesService.storeInBackground();
      }
    });
    stage.xProperty().addListener(observable -> {
      if (!stage.isMaximized()) {
        mainWindowPrefs.setX(stage.getX());
        preferencesService.storeInBackground();
      }
    });
    stage.yProperty().addListener(observable -> {
      if (!stage.isMaximized()) {
        mainWindowPrefs.setY(stage.getY());
        preferencesService.storeInBackground();
      }
    });

    ReadOnlyBooleanProperty focusedProperty = stage.focusedProperty();
    focusedProperty.removeListener(windowFocusListener);
    focusedProperty.addListener(windowFocusListener);
  }

  private void checkGamePortInBackground() {
    portCheckStatusButton.setText(i18n.get("statusBar.checkingPort"));
    portCheckService.checkGamePortInBackground().thenAccept(result -> {
      if (result) {
        portCheckStatusButton.setText(i18n.get("statusBar.portReachable"));
      } else {
        portCheckStatusButton.setText(i18n.get("statusBar.portUnreachable"));
      }
    }).exceptionally(throwable -> {
      portCheckStatusButton.setText(i18n.get("statusBar.portCheckFailed"));
      return null;
    });
  }

  @Override
  public void onFaConnected() {
    fafConnectionButton.setText(i18n.get("statusBar.fafConnected"));
  }

  @Override
  public void onFaConnecting() {
    fafConnectionButton.setText(i18n.get("statusBar.fafConnecting"));
  }

  @Override
  public void onFafDisconnected() {
    fafConnectionButton.setText(i18n.get("statusBar.fafDisconnected"));
  }

  @FXML
  void onPortCheckHelpClicked() {
    // FIXME implement
  }

  @FXML
  void onChangePortClicked() {
    // FIXME implement
  }

  @FXML
  void onEnableUpnpClicked() {
    // FIXME implement
  }

  @FXML
  void onPortCheckRetryClicked() {
    checkGamePortInBackground();
  }

  @FXML
  void onFafReconnectClicked() {
    // FIXME implement
  }

  @FXML
  void onIrcReconnectClicked() {
    // FIXME implement
  }

  @FXML
  void onNotificationsButtonClicked() {
    Bounds screenBounds = notificationsButton.localToScreen(notificationsButton.getBoundsInLocal());
    persistentNotificationsPopup.show(notificationsButton.getScene().getWindow(), screenBounds.getMaxX(), screenBounds.getMaxY());
  }

  @FXML
  void onSupportItemSelected() {
    // FIXME implement
  }

  @FXML
  void onSettingsItemSelected() {
    Stage stage = new Stage(StageStyle.UNDECORATED);
    stage.initOwner(mainRoot.getScene().getWindow());

    Region root = settingsController.getRoot();
    sceneFactory.createScene(stage, root, true, CLOSE);

    stage.setTitle(i18n.get("settings.windowTitle"));
    stage.show();
  }

  @FXML
  void onExitItemSelected() {
    Platform.exit();
  }

  @Override
  public CompletableFuture<Path> onChoseGameDirectory() {
    CompletableFuture<Path> future = new CompletableFuture<>();
    Platform.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setTitle(i18n.get("missingGamePath.locate"));
      File result = directoryChooser.showDialog(getRoot().getScene().getWindow());

      if (result == null) {
        future.complete(null);
      } else {
        future.complete(result.toPath());
      }
    });
    return future;
  }

  public Pane getRoot() {
    return mainRoot;
  }

  @FXML
  void onCommunitySelected(ActionEvent event) {
    setActiveNavigationButton((ButtonBase) event.getSource());
    selectLastChildOrFirstItem(communityButton);
  }

  /**
   * Sets the specified button to active state.
   */
  private void setActiveNavigationButton(ButtonBase button) {
    mainNavigation.getChildren().stream()
        .filter(navigationButton -> navigationButton instanceof ButtonBase && navigationButton != button)
        .forEach(navigationItem -> navigationItem.pseudoClassStateChanged(NAVIGATION_ACTIVE_PSEUDO_CLASS, false));
    button.pseudoClassStateChanged(NAVIGATION_ACTIVE_PSEUDO_CLASS, true);

    preferencesService.getPreferences().getMainWindow().setLastView(button.getId());
    preferencesService.storeInBackground();
  }

  /**
   * Selects the previously selected child view for the given button. If no match was found (or there hasn't been any
   * previous selected view), the first item is selected.
   */
  private void selectLastChildOrFirstItem(SplitMenuButton button) {
    String lastChildView = preferencesService.getPreferences().getMainWindow().getLastChildViews().get(button.getId());

    if (lastChildView == null) {
      button.getItems().get(0).fire();
      return;
    }

    button.getItems().stream()
        .filter(item -> item.getId().equals(lastChildView))
        .forEach(MenuItem::fire);
  }

  @FXML
  void onVaultSelected(ActionEvent event) {
    setActiveNavigationButton((ButtonBase) event.getSource());
    selectLastChildOrFirstItem(vaultButton);
  }

  @FXML
  void onLeaderboardSelected(ActionEvent event) {
    setActiveNavigationButton((ButtonBase) event.getSource());
    selectLastChildOrFirstItem(leaderboardButton);
  }

  @FXML
  void onPlaySelected(ActionEvent event) {
    setActiveNavigationButton((ButtonBase) event.getSource());
    selectLastChildOrFirstItem(playButton);
  }

  @FXML
  void onChatSelected(ActionEvent event) {
    setActiveNavigationButton((ButtonBase) event.getSource());
    setContent(chatController.getRoot());
  }

  private void setContent(Node node) {
    ObservableList<Node> children = contentPane.getChildren();

    if (!children.contains(node)) {
      children.add(node);

      AnchorPane.setTopAnchor(node, 0d);
      AnchorPane.setRightAnchor(node, 0d);
      AnchorPane.setBottomAnchor(node, 0d);
      AnchorPane.setLeftAnchor(node, 0d);
    }

    for (Node child : children) {
      child.setVisible(child == node);
    }
  }

  @FXML
  void onCommunityHubSelected(ActionEvent event) {
    setContent(communityHubController.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  /**
   * Sets the parent navigation button of the specified menu item as active. This only works of the child item was
   * selected manually by the user using the dropdown menu.
   */
  private void setActiveNavigationButtonFromChild(MenuItem menuItem) {
    ButtonBase navigationButton = (ButtonBase) menuItem.getParentPopup().getOwnerNode();
    if (navigationButton == null) {
      return;
    }
    setActiveNavigationButton((ButtonBase) menuItem.getParentPopup().getOwnerNode());
    preferencesService.getPreferences().getMainWindow().getLastChildViews().put(navigationButton.getId(), menuItem.getId());
    preferencesService.storeInBackground();
  }

  @FXML
  void onNewsSelected(ActionEvent event) {
    newsController.setUpIfNecessary();
    setContent(newsController.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  @FXML
  void onCastsSelected(ActionEvent event) {
    setContent(castsController.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  @FXML
  void onPlayCustomSelected(ActionEvent event) {
    setContent(gamesController.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  @FXML
  void onPlayRanked1v1Selected(ActionEvent event) {
    ranked1v1Controller.setUpIfNecessary();
    setContent(ranked1v1Controller.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  @FXML
  void onMapsSelected(ActionEvent event) {
    mapMapVaultController.setUpIfNecessary();
    setContent(mapMapVaultController.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  @FXML
  void onModsSelected(ActionEvent event) {
    modVaultController.setUpIfNecessary();
    setContent(modVaultController.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  @FXML
  void onReplaysSelected(ActionEvent event) {
    // FIXME don't load every time?
    replayVaultController.loadLocalReplaysInBackground();
    replayVaultController.loadOnlineReplaysInBackground();
    setContent(replayVaultController.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  @FXML
  void onLeaderboardRanked1v1Selected(ActionEvent event) {
    leaderboardController.setUpIfNecessary();
    setContent(leaderboardController.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  @Override
  public void onRankedMatchInfo(RankedMatchNotification rankedMatchNotification) {
    rankedMatchNotificationPane.setVisible(rankedMatchNotification.potential);
  }

  @FXML
  void onAeonButtonClicked() {
    gameService.startSearchRanked1v1(Faction.AEON);
  }

  @FXML
  void onUefButtonClicked() {
    gameService.startSearchRanked1v1(Faction.UEF);
  }

  @FXML
  void onCybranButtonClicked() {
    gameService.startSearchRanked1v1(Faction.CYBRAN);
  }

  @FXML
  void onSeraphimButtonClicked() {
    gameService.startSearchRanked1v1(Faction.SERAPHIM);
  }

  public void onUsernameButtonClicked(ActionEvent event) {
    Button button = (Button) event.getSource();

    Bounds screenBounds = button.localToScreen(button.getBoundsInLocal());
    userMenuPopup.show(button.getScene().getWindow(), screenBounds.getMaxX(), screenBounds.getMaxY());
  }

  public void selectChatTab() {
    chatButton.fire();
  }
}
