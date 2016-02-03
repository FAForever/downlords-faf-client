package com.faforever.client.main;

import com.faforever.client.cast.CastsController;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatService;
import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StageConfigurator;
import com.faforever.client.fx.WindowDecorator;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesController;
import com.faforever.client.gravatar.GravatarService;
import com.faforever.client.hub.CommunityHubController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardController;
import com.faforever.client.login.LoginController;
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
import com.faforever.client.preferences.OnChoseGameDirectoryListener;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.SettingsController;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.rankedmatch.MatchmakerMessage;
import com.faforever.client.rankedmatch.Ranked1v1Controller;
import com.faforever.client.remote.FafService;
import com.faforever.client.replay.ReplayVaultController;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
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
import org.bridj.Pointer;
import org.bridj.PointerIO;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.shell.ITaskbarList3;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MAXIMIZE_RESTORE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MINIMIZE;

public class MainController implements OnChoseGameDirectoryListener {

  private static final PseudoClass NOTIFICATION_INFO_PSEUDO_CLASS = PseudoClass.getPseudoClass("info");
  private static final PseudoClass NOTIFICATION_WARN_PSEUDO_CLASS = PseudoClass.getPseudoClass("warn");
  private static final PseudoClass NOTIFICATION_ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
  private static final PseudoClass NAVIGATION_ACTIVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("active");
  private static final PseudoClass CONNECTIVITY_PUBLIC_PSEUDO_CLASS = PseudoClass.getPseudoClass("public");
  private static final PseudoClass CONNECTIVITY_STUN_PSEUDO_CLASS = PseudoClass.getPseudoClass("stun");
  private static final PseudoClass CONNECTIVITY_BLOCKED_PSEUDO_CLASS = PseudoClass.getPseudoClass("blocked");
  private static final PseudoClass CONNECTIVITY_UNKNOWN_PSEUDO_CLASS = PseudoClass.getPseudoClass("unknown");
  private static final PseudoClass CONNECTIVITY_CONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("connected");
  private static final PseudoClass CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("disconnected");

  @FXML
  Label chatConnectionStatusIcon;
  @FXML
  Label fafConnectionStatusIcon;
  @FXML
  Label portCheckStatusIcon;
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
  MenuButton chatConnectionButton;
  @FXML
  Label taskProgressLabel;
  @FXML
  Pane rankedMatchNotificationPane;

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
  StageConfigurator stageConfigurator;
  @Resource
  ConnectivityService connectivityService;
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
  @Resource
  LoginController loginController;
  @Resource
  FafService fafService;
  @Resource
  ChatService chatService;
  @Resource
  ExecutorService executorService;

  @Value("${mainWindowTitle}")
  String mainWindowTitle;

  @VisibleForTesting
  Popup persistentNotificationsPopup;
  private Popup userMenuPopup;
  private ChangeListener<Boolean> windowFocusListener;
  private Popup transientNotificationsPopup;
  private ITaskbarList3 taskBarList;
  private Pointer taskBarRelatedPointer;

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

        updateTaskbarProgress(null);
        return;
      }

      taskProgressBar.setVisible(true);
      taskProgressBar.progressProperty().bind(task.progressProperty());

      taskProgressLabel.setVisible(true);
      taskProgressLabel.textProperty().bind(task.titleProperty());

      updateTaskbarProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
      task.progressProperty().addListener((observable, oldValue, newValue) -> {
        updateTaskbarProgress(newValue.doubleValue());
      });
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

  /**
   * Updates the progress in the Windows 7+ task bar, if available.
   */
  private void updateTaskbarProgress(@Nullable Double progress) {
    if (taskBarRelatedPointer == null) {
      return;
    }
    executorService.execute(() -> {
      if (progress == null) {
        taskBarList.SetProgressState(taskBarRelatedPointer, ITaskbarList3.TbpFlag.TBPF_NOPROGRESS);
      } else if (progress == ProgressIndicator.INDETERMINATE_PROGRESS) {
        taskBarList.SetProgressState(taskBarRelatedPointer, ITaskbarList3.TbpFlag.TBPF_INDETERMINATE);
      } else {
        taskBarList.SetProgressState(taskBarRelatedPointer, ITaskbarList3.TbpFlag.TBPF_NORMAL);
        taskBarList.SetProgressValue(taskBarRelatedPointer, (int) (progress * 100), 100);
      }
    });
  }

  @PostConstruct
  void postConstruct() {
    // We need to initialize all skins, so initially add the chat root to the scene graph.
    setContent(chatController.getRoot());

    fafService.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> {
        switch (newValue) {
          case DISCONNECTED:
            fafConnectionButton.setText(i18n.get("statusBar.fafDisconnected"));
            fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false);
            fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, true);
            break;
          case CONNECTING:
            fafConnectionButton.setText(i18n.get("statusBar.fafConnecting"));
            fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false);
            fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false);
            break;
          case CONNECTED:
            fafConnectionButton.setText(i18n.get("statusBar.fafConnected"));
            fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, true);
            fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false);
            break;
        }
      });
    });

    chatService.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> {
        chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false);
        chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false);
        switch (newValue) {
          case DISCONNECTED:
            chatConnectionButton.setText(i18n.get("statusBar.chatDisconnected"));
            chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, true);
            break;
          case CONNECTING:
            chatConnectionButton.setText(i18n.get("statusBar.chatConnecting"));
            break;
          case CONNECTED:
            chatConnectionButton.setText(i18n.get("statusBar.chatConnected"));
            chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, true);
            break;
        }
      });
    });

    connectivityService.connectivityStateProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> {
        portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_PUBLIC_PSEUDO_CLASS, false);
        portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_STUN_PSEUDO_CLASS, false);
        portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_BLOCKED_PSEUDO_CLASS, false);
        portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_UNKNOWN_PSEUDO_CLASS, false);
        switch (newValue) {
          case PUBLIC:
            portCheckStatusIcon.setText("\uF111");
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_PUBLIC_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.connectivityPublic"));
            break;
          case STUN:
            portCheckStatusIcon.setText("\uF06A");
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_STUN_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.connectivityStun"));
            break;
          case BLOCKED:
            portCheckStatusIcon.setText("\uF056");
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_BLOCKED_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.portUnreachable"));
            break;
          case RUNNING:
            portCheckStatusIcon.setText("\uF059");
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_UNKNOWN_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.checkingPort"));
            break;
          case UNKNOWN:
            portCheckStatusIcon.setText("\uF059");
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_UNKNOWN_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.connectivityUnknown"));
            break;
          default:
            throw new AssertionError("Uncovered value: " + newValue);
        }
      });
    });

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
    gameService.addOnRankedMatchNotificationListener(this::onRankedMatchInfo);

    userService.loggedInProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        onLoggedIn();
      } else {
        onLoggedOut();
      }
    });
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

    stageConfigurator.configureScene(userInfoWindow, controller.getRoot(), false, CLOSE);

    userInfoWindow.show();
  }

  private void onLoggedIn() {
    Platform.runLater(this::enterLoggedInState);
  }

  private void onLoggedOut() {
    Platform.runLater(this::enterLoggedOutState);
  }

  public void display() {
    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
    stage.setWidth(mainWindowPrefs.getWidth());
    stage.setHeight(mainWindowPrefs.getHeight());
    stage.show();

    initWindowsTaskBar();
    enterLoggedOutState();

    if (mainWindowPrefs.getX() < 0 && mainWindowPrefs.getY() < 0) {
      JavaFxUtil.centerOnScreen(stage);
    } else {
      stage.setX(mainWindowPrefs.getX());
      stage.setY(mainWindowPrefs.getY());
    }
    if (mainWindowPrefs.getMaximized()) {
      WindowDecorator.maximize(stage);
    }
    registerWindowListeners();
  }

  /**
   * Initializes the Windows 7+ task bar.
   */
  private void initWindowsTaskBar() {
    try {
      executorService.execute(() -> {
        try {
          taskBarList = COMRuntime.newInstance(ITaskbarList3.class);

        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      });

      long hwndVal = com.sun.glass.ui.Window.getWindows().get(0).getNativeWindow();
      taskBarRelatedPointer = Pointer.pointerToAddress(hwndVal, (PointerIO) null);

    } catch (NoClassDefFoundError e) {
      taskBarRelatedPointer = null;
    }
  }

  private void enterLoggedOutState() {
    loginController.display();
    stage.setTitle(i18n.get("login.title"));
  }

  private void registerWindowListeners() {
    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
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

  private void enterLoggedInState() {
    stageConfigurator.configureScene(stage, mainRoot, true, MINIMIZE, MAXIMIZE_RESTORE, CLOSE);
    stage.setTitle(mainWindowTitle);

    gameUpdateService.checkForUpdateInBackground();
    clientUpdateService.checkForUpdateInBackground();

    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
    restoreLastView(mainWindowPrefs);

    usernameButton.setText(userService.getUsername());
    // TODO no more e-mail address :(
//    userImageView.setImage(gravatarService.getGravatar(userService.getEmail()));
    userImageView.setImage(IdenticonUtil.createIdenticon(userService.getUid()));
  }

  private void restoreLastView(WindowPrefs mainWindowPrefs) {
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

  @FXML
  void onPortCheckHelpClicked() {
    // FIXME implement
  }

  @FXML
  void onChangePortClicked() {
    // FIXME implement
  }

  @FXML
  void onPortCheckRetryClicked() {
    connectivityService.checkConnectivity();
  }

  @FXML
  void onFafReconnectClicked() {
    fafService.reconnect();
  }

  @FXML
  void onChatReconnectClicked() {
    chatService.reconnect();
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
    stageConfigurator.configureScene(stage, root, true, CLOSE);

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
      directoryChooser.setTitle(i18n.get("missingGamePath.chooserTitle"));
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

  @FXML
  void onCommunityHubSelected(ActionEvent event) {
    setContent(communityHubController.getRoot());
    setActiveNavigationButtonFromChild((MenuItem) event.getTarget());
  }

  /**
   * Sets the parent navigation button of the specified menu item as active.
   */
  private void setActiveNavigationButtonFromChild(MenuItem menuItem) {
    ChangeListener<Node> ownerNodeChangeListener = new ChangeListener<Node>() {
      @Override
      public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newValue) {
        setActiveNavigationButton((ButtonBase) newValue);
        preferencesService.getPreferences().getMainWindow().getLastChildViews().put(newValue.getId(), menuItem.getId());
        preferencesService.storeInBackground();
        observable.removeListener(this);
      }
    };

    ChangeListener<ContextMenu> parentPopupChangeListener = new ChangeListener<ContextMenu>() {
      @Override
      public void changed(ObservableValue<? extends ContextMenu> observable, ContextMenu oldValue, ContextMenu newValue) {
        newValue.ownerNodeProperty().addListener(ownerNodeChangeListener);
        observable.removeListener(this);
      }
    };
    menuItem.parentPopupProperty().addListener(parentPopupChangeListener);
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

  private void onRankedMatchInfo(MatchmakerMessage matchmakerServerMessage) {
    rankedMatchNotificationPane.setVisible(matchmakerServerMessage.potential);
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
