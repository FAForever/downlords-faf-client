package com.faforever.client.main;

import ch.micheljung.fxborderlessscene.borderless.BorderlessScene;
import com.faforever.client.chat.event.UnreadPrivateMessageEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GamePathHandler;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.main.event.Open1v1Event;
import com.faforever.client.news.UnreadNewsEvent;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.ImmediateNotificationController;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.notification.TransientNotificationsController;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.preferences.ui.SettingsController;
import com.faforever.client.rankedmatch.MatchmakerMessage;
import com.faforever.client.rankedmatch.MatchmakerMessage.MatchmakerQueue.QueueName;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.faforever.client.user.event.LoggedInEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.client.vault.VaultFileSystemLocationChecker;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.jfoenix.animation.alert.JFXAlertAnimation;
import com.jfoenix.controls.JFXAlert;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.github.nocatch.NoCatch.noCatch;
import static javafx.application.Platform.runLater;
import static javafx.scene.layout.Background.EMPTY;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
// TODO divide and conquer
public class MainController implements Controller<Node> {
  private static final PseudoClass NOTIFICATION_INFO_PSEUDO_CLASS = PseudoClass.getPseudoClass("info");
  private static final PseudoClass NOTIFICATION_WARN_PSEUDO_CLASS = PseudoClass.getPseudoClass("warn");
  private static final PseudoClass NOTIFICATION_ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
  @VisibleForTesting
  protected static final PseudoClass MAIN_WINDOW_RESTORED = PseudoClass.getPseudoClass("restored");
  private final Cache<NavigationItem, AbstractViewController<?>> viewCache;
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final GameService gameService;
  private final UiService uiService;
  private final EventBus eventBus;
  private final GamePathHandler gamePathHandler;
  private final PlatformService platformService;
  private final VaultFileSystemLocationChecker vaultFileSystemLocationChecker;
  private final ApplicationEventPublisher applicationEventPublisher;
  private String mainWindowTitle;
  private int ratingBeta;
  public Pane mainHeaderPane;
  public Labeled notificationsBadge;
  public Pane contentPane;
  public ToggleButton newsButton;
  public ToggleButton chatButton;
  public ToggleButton playButton;
  public ToggleButton tutorialsButton;
  public ToggleButton vaultButton;
  public ToggleButton leaderboardsButton;
  public ToggleButton tournamentsButton;
  public ToggleButton unitsButton;
  public Pane mainRootContent;
  public StackPane contentWrapperPane;
  public ToggleGroup mainNavigation;
  public StackPane mainRoot;
  @VisibleForTesting
  protected Popup transientNotificationsPopup;
  @VisibleForTesting
  Popup persistentNotificationsPopup;
  private NavigationItem currentItem;
  private BorderlessScene mainScene;

  @Inject
  public MainController(PreferencesService preferencesService, I18n i18n,
                        NotificationService notificationService, PlayerService playerService,
                        GameService gameService, UiService uiService, EventBus eventBus,
                        GamePathHandler gamePathHandler, PlatformService platformService,
                        VaultFileSystemLocationChecker vaultFileSystemLocationChecker,
                        ClientProperties clientProperties, ApplicationEventPublisher applicationEventPublisher) {
    this.preferencesService = preferencesService;
    this.i18n = i18n;
    this.notificationService = notificationService;
    this.playerService = playerService;
    this.gameService = gameService;
    this.uiService = uiService;
    this.eventBus = eventBus;
    this.gamePathHandler = gamePathHandler;
    this.platformService = platformService;
    this.vaultFileSystemLocationChecker = vaultFileSystemLocationChecker;
    this.applicationEventPublisher = applicationEventPublisher;
    this.viewCache = CacheBuilder.newBuilder().build();
    this.mainWindowTitle = clientProperties.getMainWindowTitle();
    this.ratingBeta = clientProperties.getTrueSkill().getBeta();
  }

  public void initialize() {
    newsButton.setUserData(NavigationItem.NEWS);
    chatButton.setUserData(NavigationItem.CHAT);
    playButton.setUserData(NavigationItem.PLAY);
    vaultButton.setUserData(NavigationItem.VAULT);
    leaderboardsButton.setUserData(NavigationItem.LEADERBOARD);
    tournamentsButton.setUserData(NavigationItem.TOURNAMENTS);
    unitsButton.setUserData(NavigationItem.UNITS);
    tutorialsButton.setUserData(NavigationItem.TUTORIALS);
    eventBus.register(this);

    PersistentNotificationsController persistentNotificationsController = uiService.loadFxml("theme/persistent_notifications.fxml");
    persistentNotificationsPopup = new Popup();
    persistentNotificationsPopup.getContent().setAll(persistentNotificationsController.getRoot());
    persistentNotificationsPopup.setAnchorLocation(AnchorLocation.CONTENT_TOP_RIGHT);
    persistentNotificationsPopup.setAutoFix(true);
    persistentNotificationsPopup.setAutoHide(true);

    TransientNotificationsController transientNotificationsController = uiService.loadFxml("theme/transient_notifications.fxml");
    transientNotificationsPopup = new Popup();
    transientNotificationsPopup.setAutoFix(true);
    transientNotificationsPopup.getScene().getRoot().getStyleClass().add("transient-notification");
    transientNotificationsPopup.getContent().setAll(transientNotificationsController.getRoot());

    transientNotificationsController.getRoot().getChildren().addListener(new ToastDisplayer(transientNotificationsController));

    updateNotificationsButton(Collections.emptyList());
    notificationService.addPersistentNotificationListener(change -> runLater(() -> updateNotificationsButton(change.getSet())));
    notificationService.addImmediateNotificationListener(notification -> runLater(() -> displayImmediateNotification(notification)));
    notificationService.addTransientNotificationListener(notification -> runLater(() -> transientNotificationsController.addNotification(notification)));
    gameService.addOnRankedMatchNotificationListener(this::onMatchmakerMessage);
    // Always load chat immediately so messages or joined channels don't need to be cached until we display them.
    getView(NavigationItem.CHAT);

    listenOnMinimizedToSetExtraDragBar();
    vaultFileSystemLocationChecker.checkVaultFileSystemLocation();
  }

  private void listenOnMinimizedToSetExtraDragBar() {
    WindowPrefs windowPrefs = preferencesService.getPreferences()
        .getMainWindow();
    InvalidationListener invalidationListener = observable -> mainHeaderPane.pseudoClassStateChanged(MAIN_WINDOW_RESTORED, !windowPrefs.getMaximized());
    JavaFxUtil.addListener(windowPrefs.maximizedProperty(), invalidationListener);
    invalidationListener.invalidated(windowPrefs.maximizedProperty());
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
    runLater(this::enterLoggedInState);
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    runLater(this::enterLoggedOutState);
  }

  @Subscribe
  public void onUnreadNews(UnreadNewsEvent event) {
    runLater(() -> newsButton.pseudoClassStateChanged(HIGHLIGHTED, event.hasUnreadNews()));
  }

  @Subscribe
  public void onUnreadMessage(UnreadPrivateMessageEvent event) {
    runLater(() -> chatButton.pseudoClassStateChanged(HIGHLIGHTED, !currentItem.equals(NavigationItem.CHAT)));
  }

  private void displayView(AbstractViewController<?> controller, NavigateEvent navigateEvent) {
    Node node = controller.getRoot();
    ObservableList<Node> children = contentPane.getChildren();

    if (!children.contains(node)) {
      children.add(node);
      JavaFxUtil.setAnchors(node, 0d);
    }

    Optional.ofNullable(currentItem).ifPresent(item -> getView(item).hide());
    controller.display(navigateEvent);
  }

  private Rectangle2D getTransientNotificationAreaBounds() {
    ObservableList<Screen> screens = Screen.getScreens();

    int toastScreenIndex = preferencesService.getPreferences().getNotification().getToastScreen();
    Screen screen;
    if (toastScreenIndex < screens.size()) {
      screen = screens.get(Math.max(0, toastScreenIndex));
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

    int size = notifications.size();
    notificationsBadge.setVisible(size != 0);
    notificationsBadge.setText(i18n.number(size));

    Severity highestSeverity = notifications.stream()
        .map(PersistentNotification::getSeverity)
        .max(Enum::compareTo)
        .orElse(null);

    notificationsBadge.pseudoClassStateChanged(NOTIFICATION_INFO_PSEUDO_CLASS, highestSeverity == Severity.INFO);
    notificationsBadge.pseudoClassStateChanged(NOTIFICATION_WARN_PSEUDO_CLASS, highestSeverity == Severity.WARN);
    notificationsBadge.pseudoClassStateChanged(NOTIFICATION_ERROR_PSEUDO_CLASS, highestSeverity == Severity.ERROR);

    FadeTransition ft = new FadeTransition(Duration.millis(666), notificationsBadge);
    ft.setFromValue(0);
    ft.setToValue(1.0);
    ft.setCycleCount(1);
    ft.setAutoReverse(true);
    ft.play();
  }

  /**
   * Hides the install4j splash screen. The hide method is invoked via reflection to accommodate starting the client
   * without install4j (e.g. on linux).
   */
  private static void hideSplashScreen() {
    try {
      final Class splashScreenClass = Class.forName("com.install4j.api.launcher.SplashScreen");
      final Method hideMethod = splashScreenClass.getDeclaredMethod("hide");
      hideMethod.invoke(null);
    } catch (ClassNotFoundException e) {
      log.debug("No install4j splash screen found to close.");
    } catch (NoSuchMethodException | IllegalAccessException e) {
      log.error("Couldn't close install4j splash screen.", e);
    } catch (InvocationTargetException e) {
      log.error("Couldn't close install4j splash screen.", e.getCause());
    }
  }

  private void onMatchmakerMessage(MatchmakerMessage message) {
    if (message.getQueues() == null
        || gameService.gameRunningProperty().get()
        || gameService.searching1v1Property().get()
        || !preferencesService.getPreferences().getNotification().getLadder1v1ToastEnabled()
        || !playerService.getCurrentPlayer().isPresent()) {
      return;
    }

    Player currentPlayer = playerService.getCurrentPlayer().get();

    int deviationFor80PercentQuality = (int) (ratingBeta / 2.5f);
    int deviationFor75PercentQuality = (int) (ratingBeta / 1.25f);
    float leaderboardRatingDeviation = currentPlayer.getLeaderboardRatingDeviation();

    Function<MatchmakerMessage.MatchmakerQueue, List<RatingRange>> ratingRangesSupplier;
    if (leaderboardRatingDeviation <= deviationFor80PercentQuality) {
      ratingRangesSupplier = MatchmakerMessage.MatchmakerQueue::getBoundary80s;
    } else if (leaderboardRatingDeviation <= deviationFor75PercentQuality) {
      ratingRangesSupplier = MatchmakerMessage.MatchmakerQueue::getBoundary75s;
    } else {
      return;
    }

    float leaderboardRatingMean = currentPlayer.getLeaderboardRatingMean();
    boolean showNotification = false;
    for (MatchmakerMessage.MatchmakerQueue matchmakerQueue : message.getQueues()) {
      if (!Objects.equals(QueueName.LADDER_1V1, matchmakerQueue.getQueueName())) {
        continue;
      }
      List<RatingRange> ratingRanges = ratingRangesSupplier.apply(matchmakerQueue);

      for (RatingRange ratingRange : ratingRanges) {
        if (ratingRange.getMin() <= leaderboardRatingMean && leaderboardRatingMean <= ratingRange.getMax()) {
          showNotification = true;
          break;
        }
      }
    }

    if (!showNotification) {
      return;
    }

    notificationService.addNotification(new TransientNotification(
        i18n.get("ranked1v1.notification.title"),
        i18n.get("ranked1v1.notification.message"),
        uiService.getThemeImage(UiService.LADDER_1V1_IMAGE),
        event -> eventBus.post(new Open1v1Event())
    ));
  }

  public void display() {
    eventBus.post(UpdateApplicationBadgeEvent.ofNewValue(0));

    Stage stage = StageHolder.getStage();
    setBackgroundImage(preferencesService.getPreferences().getMainWindow().getBackgroundImagePath());
    mainScene = uiService.createScene(stage, mainRoot);
    stage.setScene(mainScene);

    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
    double x = mainWindowPrefs.getX();
    double y = mainWindowPrefs.getY();
    int width = mainWindowPrefs.getWidth();
    int height = mainWindowPrefs.getHeight();

    stage.setMinWidth(10);
    stage.setMinHeight(10);
    stage.setWidth(width);
    stage.setHeight(height);
    stage.show();

    hideSplashScreen();
    enterLoggedOutState();

    ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(x, y, width, height);
    if (screensForRectangle.isEmpty()) {
      JavaFxUtil.centerOnScreen(stage);
    } else {
      stage.setX(x);
      stage.setY(y);
    }
    if (mainWindowPrefs.getMaximized()) {
      getMainScene().maximizeStage();
    }
    registerWindowListeners();
  }

  private void enterLoggedOutState() {
    Stage stage = StageHolder.getStage();
    stage.setTitle(i18n.get("login.title"));
    LoginController loginController = uiService.loadFxml("theme/login.fxml");

    getMainScene().setContent(loginController.getRoot());
    getMainScene().setMoveControl(loginController.getRoot());
    loginController.display();
  }

  private BorderlessScene getMainScene() {
    if (mainScene == null) {
      throw new IllegalStateException("'borderlessScene' isn't initialized yet, make sure to call display() first");
    }
    return mainScene;
  }

  private void registerWindowListeners() {
    Stage stage = StageHolder.getStage();
    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
    JavaFxUtil.addListener(mainScene.maximizedProperty(), (observable, oldValue, newValue) -> {
      if (!newValue) {
        stage.setWidth(mainWindowPrefs.getWidth());
        stage.setHeight(mainWindowPrefs.getHeight());
        ObservableList<Screen> screensForRectangle = Screen.
            getScreensForRectangle(mainWindowPrefs.getX(), mainWindowPrefs.getY(), mainWindowPrefs.getWidth(), mainWindowPrefs.getHeight());
        if (screensForRectangle.isEmpty()) {
          JavaFxUtil.centerOnScreen(stage);
        } else {
          stage.setX(mainWindowPrefs.getX());
          stage.setY(mainWindowPrefs.getY());
        }

      }
      mainWindowPrefs.setMaximized(newValue);
      preferencesService.storeInBackground();
    });
    JavaFxUtil.addListener(stage.heightProperty(), (observable, oldValue, newValue) -> {
      if (!stage.isMaximized()) {
        mainWindowPrefs.setHeight(newValue.intValue());
        preferencesService.storeInBackground();
      }
    });
    JavaFxUtil.addListener(stage.widthProperty(), (observable, oldValue, newValue) -> {
      if (!stage.isMaximized()) {
        mainWindowPrefs.setWidth(newValue.intValue());
        preferencesService.storeInBackground();
      }
    });
    JavaFxUtil.addListener(stage.xProperty(), observable -> {
      if (!stage.isMaximized()) {
        mainWindowPrefs.setX(stage.getX());
        preferencesService.storeInBackground();
      }
    });
    JavaFxUtil.addListener(stage.yProperty(), observable -> {
      if (!stage.isMaximized()) {
        mainWindowPrefs.setY(stage.getY());
        preferencesService.storeInBackground();
      }
    });
    JavaFxUtil.addListener(mainWindowPrefs.backgroundImagePathProperty(), observable -> {
      setBackgroundImage(mainWindowPrefs.getBackgroundImagePath());
    });
  }

  private void setBackgroundImage(Path filepath) {
    Image image;
    if (filepath != null && Files.exists(filepath)) {
      image = noCatch(() -> new Image(filepath.toUri().toURL().toExternalForm()));
      mainRootContent.setBackground(new Background(new BackgroundImage(
          image,
          BackgroundRepeat.NO_REPEAT,
          BackgroundRepeat.NO_REPEAT,
          BackgroundPosition.CENTER,
          new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
      )));
    } else {
      mainRootContent.setBackground(EMPTY);
    }
  }

  private void enterLoggedInState() {
    Stage stage = StageHolder.getStage();
    stage.setTitle(mainWindowTitle);
    getMainScene().setContent(mainRoot);
    getMainScene().setMoveControl(mainRoot);

    applicationEventPublisher.publishEvent(new LoggedInEvent());

    gamePathHandler.detectAndUpdateGamePath();
    restoreLastView();
  }

  private void restoreLastView() {
    final NavigationItem navigationItem;
    if (preferencesService.getPreferences().getRememberLastTab()) {
      final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
      navigationItem = Optional.ofNullable(NavigationItem.fromString(mainWindowPrefs.getLastView())).orElse(NavigationItem.NEWS);
    } else {
      navigationItem = NavigationItem.NEWS;
    }
    eventBus.post(new NavigateEvent(navigationItem));
  }

  public void onNotificationsButtonClicked() {
    Bounds screenBounds = notificationsBadge.localToScreen(notificationsBadge.getBoundsInLocal());
    persistentNotificationsPopup.show(notificationsBadge.getScene().getWindow(), screenBounds.getMaxX(), screenBounds.getMaxY());
  }

  public void onSettingsSelected() {
    Stage stage = new Stage(StageStyle.UNDECORATED);
    stage.initOwner(mainRoot.getScene().getWindow());

    SettingsController settingsController = uiService.loadFxml("theme/settings/settings.fxml");

    BorderlessScene borderlessScene = uiService.createScene(stage, settingsController.getRoot());
    stage.setScene(borderlessScene);
    stage.showingProperty().addListener(new ChangeListener<>() {
      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) {
          stage.showingProperty().removeListener(this);
          preferencesService.storeInBackground();
        }
      }
    });

    stage.setTitle(i18n.get("settings.windowTitle"));
    stage.show();
  }

  public void onExitItemSelected() {
    Platform.exit();
  }

  public Pane getRoot() {
    return mainRoot;
  }

  public void onNavigateButtonClicked(ActionEvent event) {
    eventBus.post(new NavigateEvent((NavigationItem) ((Node) event.getSource()).getUserData()));
  }

  @Subscribe
  public void onNavigateEvent(NavigateEvent navigateEvent) {
    NavigationItem item = navigateEvent.getItem();

    AbstractViewController<?> controller = getView(item);
    displayView(controller, navigateEvent);

    mainNavigation.getToggles().stream()
        .filter(toggle -> toggle.getUserData() == navigateEvent.getItem())
        .findFirst()
        .ifPresent(toggle -> toggle.setSelected(true));

    currentItem = item;
    preferencesService.getPreferences().getMainWindow().setLastView(item.name());
    preferencesService.storeInBackground();
  }

  private AbstractViewController<?> getView(NavigationItem item) {
    return noCatch(() -> viewCache.get(item, () -> uiService.loadFxml(item.getFxmlFile())));
  }

  public void onRevealMapFolder() {
    Path mapPath = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();
    this.platformService.reveal(mapPath);
  }

  public void onRevealModFolder() {
    Path modPath = preferencesService.getPreferences().getForgedAlliance().getModsDirectory();
    this.platformService.reveal(modPath);
  }

  public void onRevealLogFolder() {
    Path logPath = preferencesService.getFafLogDirectory();
    this.platformService.reveal(logPath);
  }

  public void onRevealReplayFolder() {
    Path replayPath = preferencesService.getReplaysDirectory();
    this.platformService.reveal(replayPath);
  }

  public void onRevealGamePrefsFolder() {
    Path gamePrefsPath = preferencesService.getPreferences().getForgedAlliance().getPreferencesFile();
    this.platformService.reveal(gamePrefsPath);
  }

  public void onChat(ActionEvent actionEvent) {
    chatButton.pseudoClassStateChanged(HIGHLIGHTED, false);
    onNavigateButtonClicked(actionEvent);
  }

  private void displayImmediateNotification(ImmediateNotification notification) {
    JFXAlert<?> dialog = new JFXAlert<>((Stage) getMainScene().getWindow());

    ImmediateNotificationController controller = ((ImmediateNotificationController) uiService.loadFxml("theme/immediate_notification.fxml"))
        .setNotification(notification)
        .setCloseListener(dialog::close);

    dialog.setContent(controller.getJfxDialogLayout());
    dialog.setAnimation(JFXAlertAnimation.TOP_ANIMATION);
    dialog.show();
  }

  public void onLinksAndHelp() {
    LinksAndHelpController linksAndHelpController = uiService.loadFxml("theme/links_and_help.fxml");
    Node root = linksAndHelpController.getRoot();
    uiService.showInDialog(mainRoot, root, i18n.get("help.title"));

    root.requestFocus();
  }

  public class ToastDisplayer implements InvalidationListener {
    private final TransientNotificationsController transientNotificationsController;

    public ToastDisplayer(TransientNotificationsController transientNotificationsController) {
      this.transientNotificationsController = transientNotificationsController;
    }

    @Override
    public void invalidated(Observable observable) {
      boolean enabled = preferencesService.getPreferences().getNotification().isTransientNotificationsEnabled();
      if (transientNotificationsController.getRoot().getChildren().isEmpty() || !enabled) {
        transientNotificationsPopup.hide();
        return;
      }

      Rectangle2D visualBounds = getTransientNotificationAreaBounds();
      double anchorX = visualBounds.getMaxX() - 1;
      double anchorY = visualBounds.getMaxY() - 1;
      switch (preferencesService.getPreferences().getNotification().toastPositionProperty().get()) {
        case BOTTOM_RIGHT:
          transientNotificationsPopup.setAnchorLocation(AnchorLocation.CONTENT_BOTTOM_RIGHT);
          break;
        case TOP_RIGHT:
          transientNotificationsPopup.setAnchorLocation(AnchorLocation.CONTENT_TOP_RIGHT);
          anchorY = visualBounds.getMinY();
          break;
        case BOTTOM_LEFT:
          transientNotificationsPopup.setAnchorLocation(AnchorLocation.CONTENT_BOTTOM_LEFT);
          anchorX = visualBounds.getMinX();
          break;
        case TOP_LEFT:
          transientNotificationsPopup.setAnchorLocation(AnchorLocation.CONTENT_TOP_LEFT);
          anchorX = visualBounds.getMinX();
          anchorY = visualBounds.getMinY();
          break;
        default:
          transientNotificationsPopup.setAnchorLocation(AnchorLocation.CONTENT_BOTTOM_RIGHT);
          break;
      }
      transientNotificationsPopup.show(mainRootContent.getScene().getWindow(), anchorX, anchorY);
    }
  }
}
