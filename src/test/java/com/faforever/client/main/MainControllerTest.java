package com.faforever.client.main;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.api.SessionExpiredEvent;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.ChatController;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GamePathHandler;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.notification.TransientNotificationsController;
import com.faforever.client.play.PlayController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.ui.SettingsController;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainControllerTest extends UITest {

  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
  @Mock
  private PersistentNotificationsController persistentNotificationsController;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlatformService platformService;
  @Mock
  private PlayerService playerService;
  @Mock
  private SettingsController settingsController;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private GameService gameService;
  @Mock
  private TransientNotificationsController transientNotificationsController;
  @Mock
  private LoginController loginController;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private GamePathHandler gamePathHandler;
  @Mock
  private ChatController chatController;
  @Mock
  private PlayController playController;
  @Mock
  private Environment environment;
  private MainController instance;
  private BooleanProperty gameRunningProperty;
  private Preferences preferences;
  @Mock
  private FxStage fxStage;

  @Override
  protected boolean showStage() {
    // Don't show the stage yet as it will be done by MainController.display()
    return false;
  }

  @BeforeEach
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getTrueSkill()
        .setBeta(240)
        .setInitialMean(1500)
        .setInitialStandardDeviation(500);

    preferences = PreferencesBuilder.create().defaultValues().get();

    when(environment.getActiveProfiles()).thenReturn(ArrayUtils.EMPTY_STRING_ARRAY);

    instance = new MainController(preferencesService, i18n, notificationService, uiService, eventBus,
        gamePathHandler, platformService, clientProperties, environment);
    when(persistentNotificationsController.getRoot()).thenReturn(new Pane());
    when(transientNotificationsController.getRoot()).thenReturn(new Pane());
    when(loginController.getRoot()).thenReturn(new Pane());
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(uiService.loadFxml("theme/persistent_notifications.fxml")).thenReturn(persistentNotificationsController);
    when(uiService.loadFxml("theme/transient_notifications.fxml")).thenReturn(transientNotificationsController);
    when(uiService.loadFxml("theme/settings/settings.fxml")).thenReturn(settingsController);
    when(uiService.loadFxml("theme/login/login.fxml")).thenReturn(loginController);
    when(uiService.loadFxml("theme/chat/chat.fxml")).thenReturn(chatController);
    when(uiService.loadFxml("theme/play/play.fxml")).thenReturn(playController);
    when(uiService.createScene(any())).thenAnswer(invocation -> new Scene(invocation.getArgument(0)));

    loadFxml("theme/main.fxml", clazz -> {
      if (clazz == instance.getClass()) {
        return instance;
      }
      return mock(clazz);
    });
    when(fxStage.getStage()).thenReturn(getStage());
    when(fxStage.getNonCaptionNodes()).thenReturn(FXCollections.observableArrayList());
    doAnswer(invocation -> {
      Region root = invocation.getArgument(0);
      getScene().setRoot(root);
      return fxStage;
    }).when(fxStage).setContent(any());
    instance.setFxStage(fxStage);
    WaitForAsyncUtils.asyncFx(() -> instance.display());
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void testLogOutResets() throws Exception {
    runOnFxThreadAndWait(() -> instance.onLoginSuccessEvent(new LoginSuccessEvent()));
    runOnFxThreadAndWait(() -> instance.onNavigateEvent(new NavigateEvent(NavigationItem.PLAY)));
    runOnFxThreadAndWait(() -> instance.onLoggedOutEvent(new LoggedOutEvent()));
    runOnFxThreadAndWait(() -> instance.onLoginSuccessEvent(new LoginSuccessEvent()));
    runOnFxThreadAndWait(() -> instance.onNavigateEvent(new NavigateEvent(NavigationItem.PLAY)));
    verify(uiService, times(2)).loadFxml(NavigationItem.PLAY.getFxmlFile());
  }

  @Test
  public void testHideNotifications() throws Exception {
    JavaFxUtil.runLater(() -> instance.new ToastDisplayer(transientNotificationsController).invalidated(mock(SimpleBooleanProperty.class)));
    assertFalse(instance.transientNotificationsPopup.isShowing());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDisplay() throws Exception {
    attachToRoot();
    fakeLogin();

    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.display());

    assertTrue(getStage().isShowing());
  }

  /**
   * Attaches the instance's root to the test's root. This is necessary since some components only work properly if they
   * are attached to a window.
   */
  private void attachToRoot() {
    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().add(instance.getRoot()));
    WaitForAsyncUtils.waitForFxEvents();
  }

  private void fakeLogin() throws InterruptedException {
    instance.onLoginSuccessEvent(new LoginSuccessEvent());
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void testOnNotificationsButtonClicked() throws Exception {
    fakeLogin();
    WaitForAsyncUtils.asyncFx(instance::onNotificationsButtonClicked);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.persistentNotificationsPopup.isShowing(), is(true));
  }

  @Disabled("Causes a core dump locally on windows at least")
  @Test
  public void testOnSettingsItemSelected() throws Exception {
    fakeLogin();

    Pane root = new Pane();
    when(settingsController.getRoot()).thenReturn(root);
    WaitForAsyncUtils.waitForAsyncFx(1000, instance::onSettingsSelected);
    verify(settingsController).getRoot();
    verify(uiService).createScene(any());
  }

  @Test
  @Disabled("Needs UI for testing")
  public void testOnChoseGameDirectory() throws Exception {
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), CoreMatchers.is(instance.mainRoot));
    assertThat(instance.getRoot().getParent(), CoreMatchers.is(nullValue()));
  }

  @Test
  public void testOpenStartTabWithItemSet() throws Exception {
    preferences.getMainWindow().setNavigationItem(NavigationItem.PLAY);
    instance.openStartTab();
    verify(eventBus, times(1)).post(eq(new NavigateEvent(NavigationItem.PLAY)));
  }

  @Test
  public void testOpenStartTabWithItemNotSet() throws Exception {
    preferences.getMainWindow().setNavigationItem(null);
    instance.openStartTab();
    verify(eventBus, times(1)).post(eq(new NavigateEvent(NavigationItem.NEWS)));
    verify(notificationService, times(1)).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testOnChat() throws Exception {
    instance.chatButton.pseudoClassStateChanged(HIGHLIGHTED, true);
    instance.onChat(new ActionEvent(instance.chatButton, Event.NULL_SOURCE_TARGET));
    assertThat(instance.chatButton.getPseudoClassStates().contains(HIGHLIGHTED), is(false));

  }

  @Disabled("Test fails in certain 2 Screen setups and on github actions")
  @Test
  public void testWindowOutsideScreensGetsCentered() throws Exception {
    Rectangle2D visualBounds = Screen.getPrimary().getBounds();
    preferences.getMainWindow().setY(visualBounds.getMaxY() + 1);
    preferences.getMainWindow().setX(visualBounds.getMaxX() + 1);

    WaitForAsyncUtils.asyncFx(() -> instance.display());
    WaitForAsyncUtils.waitForFxEvents();
    fakeLogin();

    Window window = instance.getRoot().getScene().getWindow();
    Rectangle2D bounds = new Rectangle2D(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    assertTrue(Screen.getPrimary().getBounds().contains(bounds));

    // Test if maximize/restore also centers
    WaitForAsyncUtils.asyncFx(() -> {
      Stage stage = StageHolder.getStage();
      stage.setMaximized(true);
      stage.setMaximized(false);
    });
    WaitForAsyncUtils.waitForFxEvents();

    Rectangle2D newBounds = new Rectangle2D(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    assertTrue(Screen.getPrimary().getBounds().contains(newBounds));
  }

  @Test
  public void testOnRevealMapFolder() throws Exception {
    instance.onRevealMapFolder();
    verify(platformService).reveal(preferences.getForgedAlliance().getMapsDirectory());
  }

  @Test
  public void testOnRevealModFolder() throws Exception {
    instance.onRevealModFolder();
    verify(platformService).reveal(preferences.getForgedAlliance().getModsDirectory());
  }

  @Test
  public void testOnRevealLogFolder() throws Exception {
    Path expectedPath = Path.of("C:\\test\\path_log");
    when(preferencesService.getFafLogDirectory()).thenReturn(expectedPath);
    instance.onRevealLogFolder();
    verify(platformService).reveal(expectedPath);
  }

  @Test
  public void testOnSessionExpired() throws Exception {
    runOnFxThreadAndWait(() -> instance.onSessionExpiredEvent(new SessionExpiredEvent()));
    verify(uiService, times(2)).loadFxml("theme/login/login.fxml");
  }
}
