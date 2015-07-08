package com.faforever.client.main;

import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.WindowDecorator;
import com.faforever.client.game.GamesController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardController;
import com.faforever.client.legacy.OnFafDisconnectedListener;
import com.faforever.client.legacy.OnLobbyConnectedListener;
import com.faforever.client.legacy.OnLobbyConnectingListener;
import com.faforever.client.lobby.LobbyService;
import com.faforever.client.network.GamePortCheckListener;
import com.faforever.client.network.PortCheckService;
import com.faforever.client.news.NewsController;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.OnChoseGameDirectoryListener;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.SettingsController;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.JavaFxUtil;
import com.faforever.client.vault.VaultController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MAXIMIZE_RESTORE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MINIMIZE;

public class MainController implements OnLobbyConnectedListener, OnLobbyConnectingListener, OnFafDisconnectedListener, GamePortCheckListener, OnChoseGameDirectoryListener {

  private static final PseudoClass NOTIFICATION_INFO_PSEUDO_STATE = PseudoClass.getPseudoClass("info");
  private static final PseudoClass NOTIFICATION_WARN_PSEUDO_STATE = PseudoClass.getPseudoClass("warn");
  private static final PseudoClass NOTIFICATION_ERROR_PSEUDO_STATE = PseudoClass.getPseudoClass("error");


  @FXML
  Pane mainHeaderPane;

  @FXML
  ButtonBase notificationsButton;

  @FXML
  Pane contentPane;

  @FXML
  ToggleGroup mainNavigationToggleGroup;

  @FXML
  ButtonBase newsButton;

  @FXML
  ButtonBase chatButton;

  @FXML
  ButtonBase gamesButton;

  @FXML
  ButtonBase vaultButton;

  @FXML
  ButtonBase leaderboardButton;

  @FXML
  ProgressBar taskProgressBar;

  @FXML
  Pane mainRoot;

  @FXML
  MenuButton usernameButton;

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

  @Autowired
  Environment environment;

  @Autowired
  NewsController newsController;

  @Autowired
  ChatController chatController;

  @Autowired
  GamesController gamesController;

  @Autowired
  VaultController vaultController;

  @Autowired
  LeaderboardController leaderboardController;

  @Autowired
  PersistentNotificationsController persistentNotificationsController;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  LobbyService lobbyService;

  @Autowired
  PortCheckService portCheckService;

  @Autowired
  ChatService chatService;

  @Autowired
  I18n i18n;

  @Autowired
  UserService userService;

  @Autowired
  TaskService taskService;

  @Autowired
  NotificationService notificationService;

  @Autowired
  SettingsController settingsWindowController;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  PlayerService playerService;

  private Popup notificationsPopup;

  @FXML
  void initialize() {
    taskPane.managedProperty().bind(taskPane.visibleProperty());
    taskProgressBar.managedProperty().bind(taskProgressBar.visibleProperty());
    taskProgressLabel.managedProperty().bind(taskProgressLabel.visibleProperty());

    setCurrentTaskInStatusBar(null);
  }

  @PostConstruct
  void postConstruct() {
    notificationsPopup = new Popup();
    notificationsPopup.getContent().setAll(persistentNotificationsController.getRoot());
    notificationsPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT);
    notificationsPopup.setAutoFix(false);
    notificationsPopup.setAutoHide(true);

    notificationService.addPersistentNotificationListener(change -> {
      Platform.runLater(() -> updateNotificationsButton(change.getSet()));
    });

    taskService.addChangeListener(TaskGroup.NET_HEAVY, change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          addTasks(change.getAddedSubList());
        }
        if (change.wasRemoved()) {
          removeTasks(change.getRemoved());
        }
      }
    });
    portCheckStatusButton.getTooltip().setText(
        i18n.get("statusBar.portCheckTooltip", preferencesService.getPreferences().getForgedAlliance().getPort())
    );
    portCheckService.addGamePortCheckListener(this);

    preferencesService.setOnChoseGameDirectoryListener(this);
  }

  /**
   * Updates the number displayed in the notifications button and sets its CSS pseudo class based on the highest
   * notification {@code Severity} of all current notifications.
   */
  private void updateNotificationsButton(Collection<? extends PersistentNotification> notifications) {
    JavaFxUtil.assertApplicationThread();

    int numberOfNotifications = notifications.size();
    notificationsButton.setText(String.valueOf(numberOfNotifications));

    Severity highestSeverity = null;
    for (PersistentNotification notification : notifications) {
      if (highestSeverity == null || notification.getSeverity().compareTo(highestSeverity) > 0) {
        highestSeverity = notification.getSeverity();
      }
    }

    notificationsButton.pseudoClassStateChanged(NOTIFICATION_INFO_PSEUDO_STATE, highestSeverity == Severity.INFO);
    notificationsButton.pseudoClassStateChanged(NOTIFICATION_WARN_PSEUDO_STATE, highestSeverity == Severity.WARN);
    notificationsButton.pseudoClassStateChanged(NOTIFICATION_ERROR_PSEUDO_STATE, highestSeverity == Severity.ERROR);
  }

  private void removeTasks(List<? extends PrioritizedTask<?>> removed) {
    setCurrentTaskInStatusBar(null);
  }

  /**
   * @param tasks a list of prioritized tasks, sorted by priority (lowest first)
   */
  private void addTasks(List<? extends PrioritizedTask<?>> tasks) {
//    List<Node> taskPanes = new ArrayList<>();
//
//    for (PrioritizedTask<?> taskPane : tasks) {
//      taskPanes.add(new Pane());
//    }
//
//    taskPane.getChildren().setAll(taskPanes);

    setCurrentTaskInStatusBar(tasks.get(tasks.size() - 1));
  }

  /**
   * @param task the task to set, {@code null} to unset
   */
  private void setCurrentTaskInStatusBar(PrioritizedTask<?> task) {
    if (task == null) {
      taskProgressBar.setVisible(false);
      taskProgressLabel.setVisible(false);
      return;
    }

    taskProgressBar.setVisible(true);
    taskProgressBar.progressProperty().bind(task.progressProperty());

    taskProgressLabel.setVisible(true);
    taskProgressLabel.setText(task.getTitle());
  }

  public void display(Stage stage) {
    lobbyService.setOnFafConnectedListener(this);
    lobbyService.setOnLobbyConnectingListener(this);
    lobbyService.setOnFafDisconnectedListener(this);

    chatService.connect();

    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();

    sceneFactory.createScene(stage, mainRoot, true, MINIMIZE, MAXIMIZE_RESTORE, CLOSE);

    stage.setTitle(environment.getProperty("mainWindowTitle"));
    restoreState(mainWindowPrefs, stage);
    stage.show();

    registerWindowListeners(stage, mainWindowPrefs);

    usernameButton.setText(userService.getUsername());

    portCheckService.checkGamePortInBackground();
  }

  private void restoreState(WindowPrefs mainWindowPrefs, Stage stage) {
    stage.setWidth(mainWindowPrefs.getWidth());
    stage.setHeight(mainWindowPrefs.getHeight());

    if (mainWindowPrefs.getMaximized()) {
      WindowDecorator.maximize(stage);
    } else {
      if (mainWindowPrefs.getX() < 0 && mainWindowPrefs.getY() < 0) {
        JavaFxUtil.centerOnScreen(stage);
      } else {
        stage.setX(mainWindowPrefs.getX());
        stage.setY(mainWindowPrefs.getY());
      }
    }


    String lastView = mainWindowPrefs.getLastView();
    if (lastView != null) {
      mainNavigationToggleGroup.getToggles().stream()
          .filter(button -> button instanceof ToggleButton)
          .filter(navigationItem -> lastView.equals(((Node) navigationItem).getId()))
          .forEach(navigationItem -> {
            ((ToggleButton) navigationItem).fire();
          });
    } else {
      newsButton.fire();
    }
  }

  @FXML
  void onNavigationButton(ActionEvent event) {
    ToggleButton button = (ToggleButton) event.getSource();
    preferencesService.getPreferences().getMainWindow().setLastView(button.getId());
    preferencesService.storeInBackground();

    if (!button.isSelected()) {
      button.setSelected(true);
    }

    Bounds boundsInLocal = button.getBoundsInLocal();
    Bounds buttonBoundsInScene = button.localToScene(boundsInLocal);

    // TODO let the component initialize themselves instead of calling a setUp method
    if (button == newsButton) {
      newsController.setUpIfNecessary();
      setContent(newsController.getRoot());
    } else if (button == chatButton) {
      setContent(chatController.getRoot());
    } else if (button == gamesButton) {
      gamesController.setUpIfNecessary();
      setContent(gamesController.getRoot());
    } else if (button == vaultButton) {
      vaultController.setUpIfNecessary();
      setContent(vaultController.getRoot());
    } else if (button == leaderboardButton) {
      leaderboardController.setUpIfNecessary();
      setContent(leaderboardController.getRoot());
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
  void onPortCheckHelpClicked(ActionEvent event) {
    // FIXME implement
  }

  @FXML
  void onChangePortClicked(ActionEvent event) {
    // FIXME implement
  }

  @FXML
  void onEnableUpnpClicked(ActionEvent event) {
    // FIXME implement
  }

  @FXML
  void onPortCheckRetryClicked(ActionEvent event) {
    portCheckService.checkGamePortInBackground();
  }

  @FXML
  void onFafReconnectClicked(ActionEvent event) {
    // FIXME implement
  }

  @FXML
  void onIrcReconnectClicked(ActionEvent event) {
    // FIXME implement
  }

  @FXML
  void onNotificationsButtonClicked(ActionEvent event) {
    Bounds screenBounds = notificationsButton.localToScreen(notificationsButton.getBoundsInLocal());
    notificationsPopup.show(notificationsButton.getScene().getWindow(), screenBounds.getMaxX(), screenBounds.getMaxY());
  }

  @Override
  public void onGamePortCheckResult(Boolean result) {
    if (result) {
      portCheckStatusButton.setText(i18n.get("statusBar.portReachable"));
    } else {
      portCheckStatusButton.setText(i18n.get("statusBar.portUnreachable"));
    }
  }

  @Override
  public void onGamePortCheckStarted() {
    portCheckStatusButton.setText(i18n.get("statusBar.checkingPort"));
  }

  public Pane getRoot() {
    return mainRoot;
  }

  @FXML
  void onSupportItemSelected(ActionEvent event) {
    // FIXME implement
  }

  @FXML
  void onSettingsItemSelected(ActionEvent event) {
    Stage stage = new Stage(StageStyle.UNDECORATED);
    stage.initOwner(mainRoot.getScene().getWindow());

    Region root = settingsWindowController.getRoot();
    sceneFactory.createScene(stage, root, true, CLOSE);

    stage.setTitle(i18n.get("settings.windowTitle"));
    stage.show();
  }

  @FXML
  void onExitItemSelected(ActionEvent event) {
    Platform.exit();
  }

  @Override
  public void onChoseGameDirectory(Callback<Path> callback) {
    Platform.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setTitle(i18n.get("missingGamePath.locate"));
      File result = directoryChooser.showDialog(getRoot().getScene().getWindow());

      if (result == null) {
        callback.success(null);
      } else {
        callback.success(result.toPath());
      }
    });
  }

  @FXML
  void onShowUserInfoClicked(ActionEvent event) {
    UserInfoWindowController userInfoWindowController = applicationContext.getBean(UserInfoWindowController.class);
    userInfoWindowController.setPlayerInfoBean(playerService.getCurrentPlayer());

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(getRoot().getScene().getWindow());

    sceneFactory.createScene(userInfoWindow, userInfoWindowController.getRoot(), true, CLOSE);

    userInfoWindow.show();
  }
}
