package com.faforever.client.main;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.FafClientApplication;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.exception.FxmlLoadException;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.headerbar.HeaderBarController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.ImmediateNotificationController;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.ServerNotificationController;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotificationsController;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.alert.Alert;
import com.faforever.client.ui.alert.animation.AlertAnimation;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.PopupUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static javafx.scene.layout.Background.EMPTY;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class MainController implements Controller<Node>, InitializingBean {

  private final Cache<NavigationItem, AbstractViewController<?>> viewCache = CacheBuilder.newBuilder().build();

  private final ClientProperties clientProperties;
  private final I18n i18n;
  private final NotificationService notificationService;
  private final UiService uiService;
  private final EventBus eventBus;
  private final LoginService loginService;
  private final Environment environment;
  private final NotificationPrefs notificationPrefs;
  private final WindowPrefs windowPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ChangeListener<Path> backgroundImageListener = (observable, oldValue, newValue) ->
      setBackgroundImage(newValue);

  public Pane contentPane;
  public StackPane contentWrapperPane;
  public StackPane mainRoot;
  public HBox headerBar;
  public HeaderBarController headerBarController;

  @VisibleForTesting
  protected Popup transientNotificationsPopup;
  private NavigationItem currentItem;
  private FxStage fxStage;
  private boolean alwaysReloadTabs;

  @Override
  public void afterPropertiesSet() {
    alwaysReloadTabs = Arrays.asList(environment.getActiveProfiles()).contains(FafClientApplication.PROFILE_RELOAD);

    loginService.loggedInProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue && oldValue) {
        viewCache.invalidateAll();
      }

      if (newValue) {
        enterLoggedInState();
      } else {
        enterLoggedOutState();
      }
    });
  }

  /**
   * Hides the install4j splash screen. The hide method is invoked via reflection to accommodate starting the client
   * without install4j (e.g. on linux).
   */
  private static void hideSplashScreen() {
    try {
      final Class<?> splashScreenClass = Class.forName("com.install4j.api.launcher.SplashScreen");
      final Method hideMethod = splashScreenClass.getDeclaredMethod("hide");
      hideMethod.invoke(null);
    } catch (ClassNotFoundException e) {
      log.info("No install4j splash screen found to close.");
    } catch (NoSuchMethodException | IllegalAccessException e) {
      log.error("Couldn't close install4j splash screen.", e);
    } catch (InvocationTargetException e) {
      log.error("Couldn't close install4j splash screen.", e.getCause());
    }
  }

  public void initialize() {
    eventBus.register(this);

    TransientNotificationsController transientNotificationsController = uiService.loadFxml("theme/transient_notifications.fxml");
    transientNotificationsPopup = PopupUtil.createPopup(transientNotificationsController.getRoot());
    transientNotificationsPopup.getScene().getRoot().getStyleClass().add("transient-notification");

    transientNotificationsController.getRoot()
        .getChildren()
        .addListener(new ToastDisplayer(transientNotificationsController));

    notificationService.addImmediateNotificationListener(notification -> fxApplicationThreadExecutor.execute(() -> displayImmediateNotification(notification)));
    notificationService.addServerNotificationListener(notification -> fxApplicationThreadExecutor.execute(() -> displayServerNotification(notification)));
    notificationService.addTransientNotificationListener(notification -> fxApplicationThreadExecutor.execute(() -> transientNotificationsController.addNotification(notification)));
    // Always load chat immediately so messages or joined channels don't need to be cached until we display them.
    getView(NavigationItem.CHAT);
  }

  private void displayView(AbstractViewController<?> controller, NavigateEvent navigateEvent) {
    Node node = controller.getRoot();
    ObservableList<Node> children = contentPane.getChildren();

    if (alwaysReloadTabs) {
      children.clear();
    }

    if (!children.contains(node)) {
      children.add(node);
      JavaFxUtil.setAnchors(node, 0d);
    }

    if (!alwaysReloadTabs) {
      Optional.ofNullable(currentItem).ifPresent(item -> getView(item).hide());
    }
    controller.display(navigateEvent);
  }

  private Rectangle2D getTransientNotificationAreaBounds() {
    ObservableList<Screen> screens = Screen.getScreens();

    int toastScreenIndex = notificationPrefs.getToastScreen();
    Screen screen;
    if (toastScreenIndex < screens.size()) {
      screen = screens.get(Math.max(0, toastScreenIndex));
    } else {
      screen = Screen.getPrimary();
    }
    return screen.getVisualBounds();
  }

  public void display() {
    eventBus.post(UpdateApplicationBadgeEvent.ofNewValue(0));

    Stage stage = StageHolder.getStage();
    setBackgroundImage(windowPrefs.getBackgroundImagePath());

    int width = windowPrefs.getWidth();
    int height = windowPrefs.getHeight();

    stage.setMinWidth(10);
    stage.setMinHeight(10);
    stage.setWidth(width);
    stage.setHeight(height);
    stage.show();

    hideSplashScreen();
    enterLoggedOutState();

    JavaFxUtil.assertApplicationThread();
    stage.setMaximized(windowPrefs.getMaximized());
    if (!stage.isMaximized()) {
      setWindowPosition(stage, windowPrefs);
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
    LoginController loginController = uiService.loadFxml("theme/login/login.fxml");

    fxApplicationThreadExecutor.execute(() -> {
      contentPane.getChildren().clear();
      fxStage.getStage().setTitle(i18n.get("login.title"));

      fxStage.setContent(loginController.getRoot());

      fxStage.getNonCaptionNodes().clear();
    });
  }

  private void registerWindowListeners() {
    Stage stage = fxStage.getStage();
    JavaFxUtil.addListener(stage.heightProperty(), (observable, oldValue, newValue) -> {
      if (!stage.isMaximized()) {
        windowPrefs.setHeight(newValue.intValue());
      }
    });
    JavaFxUtil.addListener(stage.widthProperty(), (observable, oldValue, newValue) -> {
      if (!stage.isMaximized()) {
        windowPrefs.setWidth(newValue.intValue());
      }
    });
    JavaFxUtil.addListener(stage.xProperty(), observable -> {
      if (!stage.isMaximized()) {
        windowPrefs.setX(stage.getX());
      }
    });
    JavaFxUtil.addListener(stage.yProperty(), observable -> {
      if (!stage.isMaximized()) {
        windowPrefs.setY(stage.getY());
      }
    });
    JavaFxUtil.addListener(stage.maximizedProperty(), observable -> {
      windowPrefs.setMaximized(stage.isMaximized());
      if (!stage.isMaximized()) {
        setWindowPosition(stage, windowPrefs);
      }
    });
    JavaFxUtil.addListener(windowPrefs.backgroundImagePathProperty(), new WeakChangeListener<>(backgroundImageListener));
  }

  private void setBackgroundImage(Path filepath) {
    Image image;
    if (filepath != null && Files.exists(filepath)) {
      try {
        image = new Image(filepath.toUri().toURL().toExternalForm());
        mainRoot.setBackground(new Background(new BackgroundImage(
            image,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
        )));
        return;
      } catch (MalformedURLException e) {
        throw new AssetLoadException("Could not load background image", e, "background.couldNotLoad", filepath);
      }
    }

    mainRoot.setBackground(EMPTY);
  }

  private void enterLoggedInState() {
    fxApplicationThreadExecutor.execute(() -> {
      Stage stage = StageHolder.getStage();
      stage.setTitle(clientProperties.getMainWindowTitle());

      fxStage.setContent(getRoot());
      fxStage.getNonCaptionNodes().setAll(headerBarController.getNonCaptionNodes());
      fxStage.setTitleBar(headerBar);

      openStartTab();
    });
  }

  @VisibleForTesting
  void openStartTab() {
    NavigationItem navigationItem = windowPrefs.getNavigationItem();
    if (navigationItem == null) {
      navigationItem = NavigationItem.NEWS;

      if (!windowPrefs.navigationItemProperty().isBound()) {
        askUserForPreferenceOverStartTab();
      }
    }
    eventBus.post(new NavigateEvent(navigationItem));
  }

  private void askUserForPreferenceOverStartTab() {
    windowPrefs.setNavigationItem(NavigationItem.NEWS);
    List<Action> actions = Collections.singletonList(new Action(i18n.get("startTab.configure"), event ->
        makePopUpAskingForPreferenceInStartTab()));
    notificationService.addNotification(new PersistentNotification(i18n.get("startTab.wantToConfigure"), Severity.INFO, actions));
  }

  private void makePopUpAskingForPreferenceInStartTab() {
    StartTabChooseController startTabChooseController = uiService.loadFxml("theme/start_tab_choose.fxml");
    Action saveAction = new Action(i18n.get("startTab.save"), event -> {
      NavigationItem newSelection = startTabChooseController.getSelected();
      windowPrefs.setNavigationItem(newSelection);
      eventBus.post(new NavigateEvent(newSelection));
    });
    ImmediateNotification notification =
        new ImmediateNotification(i18n.get("startTab.title"), i18n.get("startTab.message"),
            Severity.INFO, null, Collections.singletonList(saveAction), startTabChooseController.getRoot());
    notificationService.addNotification(notification);
  }

  public Pane getRoot() {
    return mainRoot;
  }

  @Subscribe
  public void onNavigateEvent(NavigateEvent navigateEvent) {
    NavigationItem item = navigateEvent.getItem();

    AbstractViewController<?> controller = getView(item);
    displayView(controller, navigateEvent);

    currentItem = item;
  }

  private AbstractViewController<?> getView(NavigationItem item) {
    if (alwaysReloadTabs) {
      return uiService.loadFxml(item.getFxmlFile());
    }

    try {
      return viewCache.get(item, () -> uiService.loadFxml(item.getFxmlFile()));
    } catch (ExecutionException e) {
      throw new FxmlLoadException("Could not load tab view", e, "view.couldNotLoad", i18n.get(item.getI18nKey()));
    }
  }

  private void displayImmediateNotification(ImmediateNotification notification) {
    Alert<?> dialog = new Alert<>(fxStage.getStage(), fxApplicationThreadExecutor);

    ImmediateNotificationController controller = ((ImmediateNotificationController) uiService.loadFxml("theme/immediate_notification.fxml"))
        .setNotification(notification)
        .setCloseListener(dialog::close);

    dialog.setContent(controller.getDialogLayout());
    dialog.setAnimation(AlertAnimation.TOP_ANIMATION);
    dialog.show();
  }

  private void displayServerNotification(ImmediateNotification notification) {
    Alert<?> dialog = new Alert<>(fxStage.getStage(), fxApplicationThreadExecutor);

    ServerNotificationController controller = ((ServerNotificationController) uiService.loadFxml("theme/server_notification.fxml"))
        .setNotification(notification)
        .setCloseListener(dialog::close);

    dialog.setContent(controller.getDialogLayout());
    dialog.setAnimation(AlertAnimation.TOP_ANIMATION);
    dialog.show();
  }

  public void setFxStage(FxStage fxWindow) {
    this.fxStage = fxWindow;
  }

  public class ToastDisplayer implements InvalidationListener {
    private final TransientNotificationsController transientNotificationsController;

    public ToastDisplayer(TransientNotificationsController transientNotificationsController) {
      this.transientNotificationsController = transientNotificationsController;
    }

    @Override
    public void invalidated(Observable observable) {
      boolean enabled = notificationPrefs.isTransientNotificationsEnabled();
      if (transientNotificationsController.getRoot().getChildren().isEmpty() || !enabled) {
        transientNotificationsPopup.hide();
        return;
      }

      Rectangle2D visualBounds = getTransientNotificationAreaBounds();
      double anchorX = visualBounds.getMaxX() - 1;
      double anchorY = visualBounds.getMaxY() - 1;
      AnchorLocation location = switch (notificationPrefs.getToastPosition()) {
        case BOTTOM_RIGHT -> AnchorLocation.CONTENT_BOTTOM_RIGHT;
        case TOP_RIGHT -> {
          anchorY = visualBounds.getMinY();
          yield AnchorLocation.CONTENT_TOP_RIGHT;
        }
        case BOTTOM_LEFT -> {
          anchorX = visualBounds.getMinX();
          yield AnchorLocation.CONTENT_BOTTOM_LEFT;
        }
        case TOP_LEFT -> {
          anchorX = visualBounds.getMinX();
          anchorY = visualBounds.getMinY();
          yield AnchorLocation.CONTENT_TOP_LEFT;
        }
      };
      transientNotificationsPopup.setAnchorLocation(location);
      transientNotificationsPopup.show(mainRoot.getScene().getWindow(), anchorX, anchorY);
    }
  }
}
