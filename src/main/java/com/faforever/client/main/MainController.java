package com.faforever.client.main;

import com.faforever.client.chat.event.UnreadPrivateMessageEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WindowController;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.LoginController;
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
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.tray.event.IncrementApplicationBadgeEvent;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowController.WindowButtonType.MAXIMIZE_RESTORE;
import static com.faforever.client.fx.WindowController.WindowButtonType.MINIMIZE;
import static com.github.nocatch.NoCatch.noCatch;
import static javafx.application.Platform.runLater;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
// TODO divide and conquer
public class MainController implements Controller<Node> {
  private static final PseudoClass NOTIFICATION_INFO_PSEUDO_CLASS = PseudoClass.getPseudoClass("info");
  private static final PseudoClass NOTIFICATION_WARN_PSEUDO_CLASS = PseudoClass.getPseudoClass("warn");
  private static final PseudoClass NOTIFICATION_ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
  private final Cache<NavigationItem, AbstractViewController<?>> viewCache;
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final GameService gameService;
  private final ClientUpdateService clientUpdateService;
  private final UiService uiService;
  private final EventBus eventBus;
  private final String mainWindowTitle;
  private final int ratingBeta;
  private final PlatformService platformService;
  public Pane mainHeaderPane;
  public Labeled notificationsBadge;
  public Pane contentPane;
  public ToggleButton newsButton;
  public ToggleButton chatButton;
  public ToggleButton playButton;
  public ToggleButton vaultButton;
  public ToggleButton leaderboardsButton;
  public ToggleButton unitsButton;
  public Pane mainRoot;
  public ToggleGroup mainNavigation;
  @VisibleForTesting
  protected Popup transientNotificationsPopup;
  @VisibleForTesting
  Popup persistentNotificationsPopup;
  private NavigationItem currentItem;
  private WindowController windowController;

  @Inject
  public MainController(PreferencesService preferencesService, I18n i18n, NotificationService notificationService,
                        PlayerService playerService, GameService gameService, ClientUpdateService clientUpdateService,
                        UiService uiService, EventBus eventBus, ClientProperties clientProperties, PlatformService platformService) {
    this.preferencesService = preferencesService;
    this.i18n = i18n;
    this.notificationService = notificationService;
    this.playerService = playerService;
    this.gameService = gameService;
    this.clientUpdateService = clientUpdateService;
    this.uiService = uiService;
    this.eventBus = eventBus;

    this.mainWindowTitle = clientProperties.getMainWindowTitle();
    this.ratingBeta = clientProperties.getTrueSkill().getBeta();
    this.platformService = platformService;
    this.viewCache = CacheBuilder.newBuilder().build();
  }

  public void initialize() {
    newsButton.setUserData(NavigationItem.NEWS);
    chatButton.setUserData(NavigationItem.CHAT);
    playButton.setUserData(NavigationItem.PLAY);
    vaultButton.setUserData(NavigationItem.VAULT);
    leaderboardsButton.setUserData(NavigationItem.LEADERBOARD);
    unitsButton.setUserData(NavigationItem.UNITS);
    eventBus.register(this);
    windowController = uiService.loadFxml("theme/window.fxml");

    PersistentNotificationsController persistentNotificationsController = uiService.loadFxml("theme/persistent_notifications.fxml");
    persistentNotificationsPopup = new Popup();
    persistentNotificationsPopup.getContent().setAll(persistentNotificationsController.getRoot());
    persistentNotificationsPopup.setAnchorLocation(AnchorLocation.CONTENT_TOP_LEFT);
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
    loadView(NavigationItem.CHAT);
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
  }

  private void displayImmediateNotification(ImmediateNotification notification) {
    ImmediateNotificationController controller = uiService.loadFxml("theme/immediate_notification.fxml");
    controller.setNotification(notification);

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(StageHolder.getStage().getOwner());

    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(userInfoWindow, controller.getRoot(), true, CLOSE, MAXIMIZE_RESTORE);

    userInfoWindow.show();
  }

  private void onMatchmakerMessage(MatchmakerMessage message) {
    if (message.getQueues() == null
        || gameService.gameRunningProperty().get()
        || !preferencesService.getPreferences().getNotification().getLadder1v1ToastEnabled()) {
      return;
    }

    Player currentPlayer = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player has not been set"));

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
      if (!Objects.equals("ladder1v1", matchmakerQueue.getQueueName())) {
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
        event -> eventBus.post(new NavigateEvent(NavigationItem.PLAY))
    ));
  }

  public void display() {
    eventBus.post(new IncrementApplicationBadgeEvent(0));
    Stage stage = StageHolder.getStage();
    windowController.configure(stage, mainRoot, true, MINIMIZE, MAXIMIZE_RESTORE, CLOSE);
    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
    double x = mainWindowPrefs.getX();
    double y = mainWindowPrefs.getY();
    int width = mainWindowPrefs.getWidth();
    int height = mainWindowPrefs.getHeight();

    stage.setWidth(width);
    stage.setHeight(height);
    stage.show();
    enterLoggedOutState();

    ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(x, y, width, height);
    if (screensForRectangle.isEmpty()) {
      JavaFxUtil.centerOnScreen(stage);
    } else {
      stage.setX(x);
      stage.setY(y);
    }
    if (mainWindowPrefs.getMaximized()) {
      WindowController.maximize(stage);
    }
    registerWindowListeners();
  }

  private void enterLoggedOutState() {
    Stage stage = StageHolder.getStage();
    stage.setTitle(i18n.get("login.title"));
    LoginController loginController = uiService.loadFxml("theme/login.fxml");
    windowController.setContent(loginController.getRoot());
    loginController.display();
  }

  private void registerWindowListeners() {
    Stage stage = StageHolder.getStage();
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
  }

  private void enterLoggedInState() {
    Stage stage = StageHolder.getStage();
    stage.setTitle(mainWindowTitle);
    windowController.setContent(mainRoot);

    clientUpdateService.checkForUpdateInBackground();

    detectAndUpdateGamePath();
    restoreLastView();
  }

  private void detectAndUpdateGamePath() {
    Path faPath = preferencesService.getPreferences().getForgedAlliance().getPath();
    if (faPath == null || Files.notExists(faPath)) {
      log.info("Game path is not specified or non-existent, trying to detect");
      preferencesService.detectGamePath();
    }
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
    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(stage, settingsController.getRoot(), true, CLOSE);
    windowController.setOnHiding(event -> preferencesService.storeInBackground());

    stage.setTitle(i18n.get("settings.windowTitle"));
    stage.show();
  }

  public void onExitItemSelected() {
    Platform.exit();
  }

  public Pane getRoot() {
    return mainRoot;
  }

  public void onNavigate(ActionEvent event) {
    eventBus.post(new NavigateEvent((NavigationItem) ((Node) event.getSource()).getUserData()));
  }

  @Subscribe
  public void onNavigateEvent(NavigateEvent navigateEvent) {
    NavigationItem item = navigateEvent.getItem();
    AbstractViewController<?> controller = loadView(item);

    setContent(controller.getRoot());

    mainNavigation.getToggles().stream()
        .filter(toggle -> toggle.getUserData() == navigateEvent.getItem())
        .findFirst()
        .ifPresent(toggle -> toggle.setSelected(true));

    currentItem = item;
    preferencesService.getPreferences().getMainWindow().setLastView(item.name());
    preferencesService.storeInBackground();
  }

  private AbstractViewController<?> loadView(NavigationItem item) {
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

  public void onChat(ActionEvent actionEvent) {
    chatButton.pseudoClassStateChanged(HIGHLIGHTED, false);
    onNavigate(actionEvent);
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
      transientNotificationsPopup.show(mainRoot.getScene().getWindow(), anchorX, anchorY);
    }
  }
}
