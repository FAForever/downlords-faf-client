package com.faforever.client.main;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.headerbar.HeaderBarController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.tray.TrayIconManager;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.faforever.client.user.LoginService;
import com.google.common.annotations.VisibleForTesting;
import javafx.collections.ObservableList;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static javafx.scene.layout.Background.EMPTY;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class MainController extends NodeController<Node> implements InitializingBean {

  private final ClientProperties clientProperties;
  private final I18n i18n;
  private final NotificationService notificationService;
  private final UiService uiService;
  private final LoginService loginService;
  private final WindowPrefs windowPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final TrayIconManager trayIconManager;
  private final NavigationHandler navigationHandler;

  public Pane contentPane;
  public StackPane contentWrapperPane;
  public StackPane mainRoot;
  public HBox headerBar;
  public HeaderBarController headerBarController;

  private FxStage fxStage;

  @Override
  public void afterPropertiesSet() {
    loginService.loggedInProperty().addListener((observable, oldValue, newValue) -> {
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

  @Override
  protected void onInitialize() {
    navigationHandler.navigationEventProperty().subscribe(this::onNavigateEvent);
  }

  private void displayView(NodeController<?> controller, NavigateEvent navigateEvent) {
    controller.display(navigateEvent);
    Node node = controller.getRoot();
    contentPane.getChildren().setAll(node);
    JavaFxUtil.setAnchors(node, 0d);
  }

  public void display() {
    trayIconManager.onSetApplicationBadgeEvent(UpdateApplicationBadgeEvent.ofNewValue(0));

    Stage stage = StageHolder.getStage();

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
    windowPrefs.backgroundImagePathProperty().when(showing).subscribe(this::setBackgroundImage);
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
    navigationHandler.navigateTo(new NavigateEvent(navigationItem));
  }

  private void askUserForPreferenceOverStartTab() {
    windowPrefs.setNavigationItem(NavigationItem.NEWS);
    List<Action> actions = Collections.singletonList(
        new Action(i18n.get("startTab.configure"), this::makePopUpAskingForPreferenceInStartTab));
    notificationService.addNotification(new PersistentNotification(i18n.get("startTab.wantToConfigure"), Severity.INFO, actions));
  }

  private void makePopUpAskingForPreferenceInStartTab() {
    StartTabChooseController startTabChooseController = uiService.loadFxml("theme/start_tab_choose.fxml");
    Action saveAction = new Action(i18n.get("startTab.save"), () -> {
      NavigationItem newSelection = startTabChooseController.getSelected();
      windowPrefs.setNavigationItem(newSelection);
      navigationHandler.navigateTo(new NavigateEvent(newSelection));
    });
    ImmediateNotification notification =
        new ImmediateNotification(i18n.get("startTab.title"), i18n.get("startTab.message"),
            Severity.INFO, null, Collections.singletonList(saveAction), startTabChooseController.getRoot());
    notificationService.addNotification(notification);
  }

  @Override
  public Pane getRoot() {
    return mainRoot;
  }

  public void onNavigateEvent(NavigateEvent navigateEvent) {
    if (navigateEvent == null) {
      return;
    }

    NavigationItem item = navigateEvent.getItem();

    NodeController<?> controller = uiService.loadFxml(item.getFxmlFile());
    displayView(controller, navigateEvent);
  }

  public void setFxStage(FxStage fxWindow) {
    this.fxStage = fxWindow;
  }

}
