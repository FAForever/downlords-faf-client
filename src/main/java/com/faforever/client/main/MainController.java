package com.faforever.client.main;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
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
import javafx.beans.binding.Bindings;
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
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
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
// TODO divide and conquer
public class MainController implements Controller<Node> {
  private static final PseudoClass NOTIFICATION_INFO_PSEUDO_CLASS = PseudoClass.getPseudoClass("info");
  private static final PseudoClass NOTIFICATION_WARN_PSEUDO_CLASS = PseudoClass.getPseudoClass("warn");
  private static final PseudoClass NOTIFICATION_ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
  private final Cache<NavigationItem, AbstractViewController<?>> viewCache;
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

  @Inject
  PreferencesService preferencesService;
  @Inject
  I18n i18n;
  @Inject
  NotificationService notificationService;
  @Inject
  PlayerService playerService;
  @Inject
  GameService gameService;
  @Inject
  ClientUpdateService clientUpdateService;
  @Inject
  Stage stage;
  @Inject
  UiService uiService;
  @Inject
  EventBus eventBus;

  @Value("${mainWindowTitle}")
  String mainWindowTitle;
  @Value("${rating.beta}")
  int ratingBeta;

  @VisibleForTesting
  Popup persistentNotificationsPopup;

  private Popup transientNotificationsPopup;
  private WindowController windowController;

  public MainController() {
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
    persistentNotificationsPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT);
    persistentNotificationsPopup.setAutoFix(false);
    persistentNotificationsPopup.setAutoHide(true);

    TransientNotificationsController transientNotificationsController = uiService.loadFxml("theme/transient_notifications.fxml");
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
      boolean enabled = preferencesService.getPreferences().getNotification().isTransientNotificationsEnabled();
      if (!transientNotificationsController.getRoot().getChildren().isEmpty() && enabled) {
        Rectangle2D visualBounds = getTransientNotificationAreaBounds();
        transientNotificationsPopup.show(mainRoot.getScene().getWindow(), visualBounds.getMaxX(), visualBounds.getMaxY());
      } else {
        transientNotificationsPopup.hide();
      }
    });

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
    userInfoWindow.initOwner(stage.getOwner());

    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(userInfoWindow, controller.getRoot(), true, CLOSE, MAXIMIZE_RESTORE);

    userInfoWindow.show();
  }

  private void onMatchmakerMessage(MatchmakerMessage message) {
    if (message.getQueues() == null
        || gameService.gameRunningProperty().get()
        || !preferencesService.getPreferences().getNotification().isRanked1v1ToastEnabled()) {
      return;
    }

    Player currentPlayer = playerService.getCurrentPlayer();

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
        uiService.getThemeImage(UiService.RANKED_1V1_IMAGE),
        event -> eventBus.post(new NavigateEvent(NavigationItem.PLAY))
    ));
  }

  public void display() {
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
    stage.setTitle(i18n.get("login.title"));
    LoginController loginController = uiService.loadFxml("theme/login.fxml");
    windowController.setContent(loginController.getRoot());
    loginController.display();
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
  }

  private void enterLoggedInState() {
    stage.setTitle(mainWindowTitle);
    windowController.setContent(mainRoot);

    clientUpdateService.checkForUpdateInBackground();

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
    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(stage, settingsController.getRoot(), true, CLOSE);

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

    preferencesService.getPreferences().getMainWindow().setLastView(item.name());
    preferencesService.storeInBackground();
  }

  private AbstractViewController<?> loadView(NavigationItem item) {
    return noCatch(() -> viewCache.get(item, () -> uiService.loadFxml(item.getFxmlFile())));
  }
}
