package com.faforever.client.main;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.chat.event.UnreadPrivateMessageEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.JoinDiscordEvent;
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
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.news.UnreadNewsEvent;
import com.faforever.client.notification.Action;
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
import com.faforever.client.rankedmatch.MatchmakerInfoMessage;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.alert.Alert;
import com.faforever.client.ui.alert.animation.AlertAnimation;
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
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
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
import javafx.stage.WindowEvent;
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
import java.util.stream.Collectors;

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
  private final String mainWindowTitle;
  private final int ratingBeta;

  public Pane mainHeaderPane;
  public Pane contentPane;
  public ToggleButton newsButton;
  public ToggleButton chatButton;
  public ToggleButton playButton;
  public ToggleButton tutorialsButton;
  public ToggleButton vaultButton;
  public ToggleButton leaderboardsButton;
  public ToggleButton tournamentsButton;
  public ToggleButton unitsButton;
  public StackPane contentWrapperPane;
  public ToggleGroup mainNavigation;
  public StackPane mainRoot;
  public Pane leftMenuPane;
  public Pane rightMenuPane;
  public Button notificationButton;
  /** Dropdown for when there is not enough room for all navigation buttons to be displayed. */
  public MenuButton navigationDropdown;

  @VisibleForTesting
  protected Popup transientNotificationsPopup;
  @VisibleForTesting
  Popup persistentNotificationsPopup;
  private NavigationItem currentItem;
  private FxStage fxStage;

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
    // Always load chat immediately so messages or joined channels don't need to be cached until we display them.
    getView(NavigationItem.CHAT);

    vaultFileSystemLocationChecker.checkVaultFileSystemLocation();
    notificationButton.managedProperty().bind(notificationButton.visibleProperty());

    navigationDropdown.getItems().setAll(createMenuItemsFromNavigation());
    navigationDropdown.managedProperty().bind(navigationDropdown.visibleProperty());

    leftMenuPane.layoutBoundsProperty().addListener(observable -> {
      navigationDropdown.setVisible(false);
      leftMenuPane.getChildrenUnmodifiable().forEach(node -> {
        Bounds boundsInParent = node.getBoundsInParent();
        // First time this is called, minY is negative. This is hacky but better than wasting time investigating.
        boolean hasSpace = boundsInParent.getMinY() < 0
            || leftMenuPane.getLayoutBounds().contains(boundsInParent.getCenterX(), boundsInParent.getCenterY());
        if (!hasSpace) {
          navigationDropdown.setVisible(true);
        }
        node.setVisible(hasSpace);
      });
    });
  }

  private List<MenuItem> createMenuItemsFromNavigation() {
    return leftMenuPane.getChildrenUnmodifiable().stream()
        .filter(menuItem -> menuItem.getUserData() instanceof NavigationItem)
        .map(menuButton -> {
          MenuItem menuItem = new MenuItem(((Labeled) menuButton).getText());
          menuItem.setOnAction(event -> eventBus.post(new NavigateEvent((NavigationItem) menuButton.getUserData())));
          return menuItem;
        })
        .collect(Collectors.toList());
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
    notificationButton.setVisible(size != 0);

    Severity highestSeverity = notifications.stream()
        .map(PersistentNotification::getSeverity)
        .max(Enum::compareTo)
        .orElse(null);

    notificationButton.pseudoClassStateChanged(NOTIFICATION_INFO_PSEUDO_CLASS, highestSeverity == Severity.INFO);
    notificationButton.pseudoClassStateChanged(NOTIFICATION_WARN_PSEUDO_CLASS, highestSeverity == Severity.WARN);
    notificationButton.pseudoClassStateChanged(NOTIFICATION_ERROR_PSEUDO_CLASS, highestSeverity == Severity.ERROR);
  }

  public void display() {
    eventBus.post(UpdateApplicationBadgeEvent.ofNewValue(0));

    Stage stage = StageHolder.getStage();
    setBackgroundImage(preferencesService.getPreferences().getMainWindow().getBackgroundImagePath());

    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
    int width = mainWindowPrefs.getWidth();
    int height = mainWindowPrefs.getHeight();

    stage.setMinWidth(10);
    stage.setMinHeight(10);
    stage.setWidth(width);
    stage.setHeight(height);
    stage.show();

    hideSplashScreen();
    enterLoggedOutState();

    JavaFxUtil.assertApplicationThread();
    stage.setMaximized(mainWindowPrefs.getMaximized());
    if (!stage.isMaximized()) {
      setWindowPosition(stage, mainWindowPrefs);
    }
    registerWindowListeners();
  }

  private void setWindowPosition(Stage stage, WindowPrefs mainWindowPrefs) {
    double x = mainWindowPrefs.getX();
    double y = mainWindowPrefs.getY();
    int width = mainWindowPrefs.getWidth();
    int height = mainWindowPrefs.getHeight();
    ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(x, y, width, height);
    if (screensForRectangle.isEmpty()) {
      JavaFxUtil.centerOnScreen(stage);
    } else {
      stage.setX(x);
      stage.setY(y);
    }
  }

  private void enterLoggedOutState() {
    fxStage.getStage().setTitle(i18n.get("login.title"));

    LoginController loginController = uiService.loadFxml("theme/login.fxml");
    fxStage.setContent(loginController.getRoot());
    loginController.display();

    fxStage.getNonCaptionNodes().clear();
  }

  private void registerWindowListeners() {
    Stage stage = fxStage.getStage();
    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();
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
    JavaFxUtil.addListener(stage.maximizedProperty(), observable -> {
      mainWindowPrefs.setMaximized(stage.isMaximized());
      preferencesService.storeInBackground();
      if (!stage.isMaximized()) {
        setWindowPosition(stage, mainWindowPrefs);
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
      mainRoot.setBackground(new Background(new BackgroundImage(
          image,
          BackgroundRepeat.NO_REPEAT,
          BackgroundRepeat.NO_REPEAT,
          BackgroundPosition.CENTER,
          new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
      )));
    } else {
      mainRoot.setBackground(EMPTY);
    }
  }

  private void enterLoggedInState() {
    Stage stage = StageHolder.getStage();
    stage.setTitle(mainWindowTitle);

    fxStage.setContent(getRoot());
    fxStage.getNonCaptionNodes().setAll(leftMenuPane, rightMenuPane, navigationDropdown);
    fxStage.setTitleBar(mainHeaderPane);

    applicationEventPublisher.publishEvent(new LoggedInEvent());

    gamePathHandler.detectAndUpdateGamePath();
    openStartTab();
  }

  @VisibleForTesting
  void openStartTab() {
    final WindowPrefs mainWindow = preferencesService.getPreferences().getMainWindow();
    NavigationItem navigationItem = mainWindow.getNavigationItem();
    if (navigationItem == null) {
      navigationItem = NavigationItem.NEWS;
      askUserForPreferenceOverStartTab(mainWindow);
    }
    eventBus.post(new NavigateEvent(navigationItem));
  }

  private void askUserForPreferenceOverStartTab(WindowPrefs mainWindow) {
    mainWindow.setNavigationItem(NavigationItem.NEWS);
    preferencesService.storeInBackground();
    List<Action> actions = Collections.singletonList(new Action(i18n.get("startTab.configure"), event -> {
      makePopUpAskingForPreferenceInStartTab(mainWindow);
    }));
    notificationService.addNotification(new PersistentNotification(i18n.get("startTab.wantToConfigure"), Severity.INFO, actions));
  }

  private void makePopUpAskingForPreferenceInStartTab(WindowPrefs mainWindow) {
    StartTabChooseController startTabChooseController = uiService.loadFxml("theme/start_tab_choose.fxml");
    Action saveAction = new Action(i18n.get("startTab.save"), event -> {
      NavigationItem newSelection = startTabChooseController.getSelected();
      mainWindow.setNavigationItem(newSelection);
      preferencesService.storeInBackground();
      eventBus.post(new NavigateEvent(newSelection));
    });
    ImmediateNotification notification =
        new ImmediateNotification(i18n.get("startTab.title"), i18n.get("startTab.message"),
            Severity.INFO, null, Collections.singletonList(saveAction), startTabChooseController.getRoot());
    notificationService.addNotification(notification);
  }

  public void onNotificationsButtonClicked() {
    Bounds screenBounds = notificationButton.localToScreen(notificationButton.getBoundsInLocal());
    persistentNotificationsPopup.show(notificationButton.getScene().getWindow(), screenBounds.getMaxX(), screenBounds.getMaxY());
  }

  public void onSettingsSelected() {
    SettingsController settingsController = uiService.loadFxml("theme/settings/settings.fxml");
    FxStage fxStage = FxStage.create(settingsController.getRoot())
        .initOwner(mainRoot.getScene().getWindow())
        .withSceneFactory(uiService::createScene)
        .allowMinimize(false)
        .apply()
        .setTitleBar(settingsController.settingsHeader);

    Stage stage = fxStage.getStage();
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
    Stage stage = fxStage.getStage();
    stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
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
    Alert<?> dialog = new Alert<>(fxStage.getStage());

    ImmediateNotificationController controller = ((ImmediateNotificationController) uiService.loadFxml("theme/immediate_notification.fxml"))
        .setNotification(notification)
        .setCloseListener(dialog::close);

    dialog.setContent(controller.getDialogLayout());
    dialog.setAnimation(AlertAnimation.TOP_ANIMATION);
    dialog.show();
  }

  public void onLinksAndHelp() {
    LinksAndHelpController linksAndHelpController = uiService.loadFxml("theme/links_and_help.fxml");
    Node root = linksAndHelpController.getRoot();
    uiService.showInDialog(mainRoot, root, i18n.get("help.title"));

    root.requestFocus();
  }

  public void setFxStage(FxStage fxWindow) {
    this.fxStage = fxWindow;
  }

  public void onDiscordButtonClicked() {
    applicationEventPublisher.publishEvent(new JoinDiscordEvent());
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
