package com.faforever.client.main;

import com.faforever.client.chat.ChatController;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WindowController;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardController;
import com.faforever.client.login.LoginController;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.notification.TransientNotificationsController;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.preferences.ui.SettingsController;
import com.faforever.client.rankedmatch.MatchmakerMessage;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Window;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainControllerTest extends AbstractPlainJavaFxTest {
  @Mock
  private PersistentNotificationsController persistentNotificationsController;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlatformService platformService;
  @Mock
  private LeaderboardController leaderboardController;
  @Mock
  private PlayerService playerService;
  @Mock
  private SettingsController settingsController;
  @Mock
  private Preferences preferences;
  @Mock
  private I18n i18n;
  @Mock
  private WindowPrefs mainWindowPrefs;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ClientUpdateService clientUpdateService;
  @Mock
  private GameService gameService;
  @Mock
  private TransientNotificationsController transientNotificationsController;
  @Mock
  private NotificationsPrefs notificationPrefs;
  @Mock
  private LoginController loginController;
  @Mock
  private WindowController windowController;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ChatController chatController;

  private MainController instance;
  private CountDownLatch mainControllerInitializedLatch;
  private BooleanProperty gameRunningProperty;

  @Before
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getTrueSkill()
        .setBeta(240)
        .setInitialMean(1500)
        .setInitialStandardDeviation(500);

    instance = new MainController(preferencesService, i18n, notificationService, playerService, gameService, clientUpdateService,
        uiService, eventBus, clientProperties, platformService);

    gameRunningProperty = new SimpleBooleanProperty();

    when(persistentNotificationsController.getRoot()).thenReturn(new Pane());
    when(transientNotificationsController.getRoot()).thenReturn(new Pane());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.loadFxml("theme/window.fxml")).thenReturn(windowController);
    when(preferences.getMainWindow()).thenReturn(mainWindowPrefs);
    when(preferences.getNotification()).thenReturn(notificationPrefs);
    when(notificationPrefs.toastPositionProperty()).thenReturn(new SimpleObjectProperty<>(ToastPosition.BOTTOM_RIGHT));
    when(notificationPrefs.getToastPosition()).thenReturn(ToastPosition.BOTTOM_RIGHT);
    when(gameService.gameRunningProperty()).thenReturn(gameRunningProperty);
    when(uiService.loadFxml("theme/persistent_notifications.fxml")).thenReturn(persistentNotificationsController);
    when(uiService.loadFxml("theme/transient_notifications.fxml")).thenReturn(transientNotificationsController);
    when(uiService.loadFxml("theme/window.fxml")).thenReturn(windowController);
    when(uiService.loadFxml("theme/settings/settings.fxml")).thenReturn(settingsController);
    when(uiService.loadFxml("theme/login.fxml")).thenReturn(loginController);
    when(uiService.loadFxml("theme/chat/chat.fxml")).thenReturn(chatController);

    loadFxml("theme/main.fxml", clazz -> {
      if (clazz == instance.getClass()) {
        return instance;
      }
      return mock(clazz);
    });

    mainControllerInitializedLatch = new CountDownLatch(1);
    // As the login check is executed AFTER the main controller has been switched to logged in state, we hook to it
    doAnswer(invocation -> {
      mainControllerInitializedLatch.countDown();
      return null;
    }).when(clientUpdateService).checkForUpdateInBackground();
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
    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().add(instance.mainRoot));
    WaitForAsyncUtils.waitForFxEvents();
  }

  private void fakeLogin() throws InterruptedException {
    instance.onLoginSuccessEvent(new LoginSuccessEvent("junit", "", 1));
    assertTrue(mainControllerInitializedLatch.await(3000, TimeUnit.SECONDS));
  }

  @Test
  public void testOnNotificationsButtonClicked() throws Exception {
    attachToRoot();
    WaitForAsyncUtils.waitForAsyncFx(1000, instance::onNotificationsButtonClicked);

    assertThat(instance.persistentNotificationsPopup.isShowing(), is(true));
  }

  @Test
  public void testOnSettingsItemSelected() throws Exception {
    attachToRoot();
    Pane root = new Pane();
    when(settingsController.getRoot()).thenReturn(root);
    WaitForAsyncUtils.waitForAsyncFx(1000, instance::onSettingsSelected);

    verify(windowController).configure(
        any(), eq(root), eq(true), eq(WindowController.WindowButtonType.CLOSE)
    );
    verify(windowController).setOnClose(any(Runnable.class));
  }

  @Test
  @Ignore("Needs UI for testing")
  public void testOnChoseGameDirectory() throws Exception {
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), CoreMatchers.is(instance.mainRoot));
    assertThat(instance.getRoot().getParent(), CoreMatchers.is(nullValue()));
  }


  @Test
  public void testOnMatchMakerMessageDisplaysNotification80Quality() {
    prepareTestMatchmakerMessageTest(100);
    verify(notificationService).addNotification(any(TransientNotification.class));
  }

  private void prepareTestMatchmakerMessageTest(float deviation) {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Consumer<MatchmakerMessage>> matchmakerMessageCaptor = ArgumentCaptor.forClass(Consumer.class);
    when(notificationPrefs.getLadder1v1ToastEnabled()).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(
        Optional.ofNullable(PlayerBuilder.create("JUnit").leaderboardRatingMean(1500).leaderboardRatingDeviation(deviation).get())
    );

    verify(gameService).addOnRankedMatchNotificationListener(matchmakerMessageCaptor.capture());

    MatchmakerMessage matchmakerMessage = new MatchmakerMessage();
    matchmakerMessage.setQueues(singletonList(new MatchmakerMessage.MatchmakerQueue("ladder1v1",
        singletonList(new RatingRange(1500, 1510)), singletonList(new RatingRange(1500, 1510)))));
    matchmakerMessageCaptor.getValue().accept(matchmakerMessage);
  }

  @Test
  public void testOnMatchMakerMessageDisplaysNotification75Quality() {
    prepareTestMatchmakerMessageTest(101);
    verify(notificationService).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testOnMatchMakerMessageDoesNotDisplaysNotificationLessThan75Quality() {
    prepareTestMatchmakerMessageTest(201);
    verify(notificationService, never()).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testOnMatchMakerMessageDoesNotDisplaysNotificationWhenGameIsRunning() {
    gameRunningProperty.set(true);
    prepareTestMatchmakerMessageTest(100);
    verify(notificationService, never()).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testOnMatchMakerMessageDisplaysNotificationNullQueues() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Consumer<MatchmakerMessage>> matchmakerMessageCaptor = ArgumentCaptor.forClass(Consumer.class);

    verify(gameService).addOnRankedMatchNotificationListener(matchmakerMessageCaptor.capture());

    MatchmakerMessage matchmakerMessage = new MatchmakerMessage();
    matchmakerMessage.setQueues(null);
    matchmakerMessageCaptor.getValue().accept(matchmakerMessage);

    verify(notificationService, never()).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testOnMatchMakerMessageDisplaysNotificationWithQueuesButDisabled() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Consumer<MatchmakerMessage>> matchmakerMessageCaptor = ArgumentCaptor.forClass(Consumer.class);
    when(notificationPrefs.getLadder1v1ToastEnabled()).thenReturn(false);

    verify(gameService).addOnRankedMatchNotificationListener(matchmakerMessageCaptor.capture());

    MatchmakerMessage matchmakerMessage = new MatchmakerMessage();
    matchmakerMessage.setQueues(singletonList(new MatchmakerMessage.MatchmakerQueue("ladder1v1",
        singletonList(new RatingRange(1500, 1510)), singletonList(new RatingRange(1500, 1510)))));
    matchmakerMessageCaptor.getValue().accept(matchmakerMessage);

    verify(notificationService, never()).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testWindowOutsideScreensGetsCentered() throws Exception {
    Rectangle2D visualBounds = Screen.getPrimary().getBounds();
    when(mainWindowPrefs.getY()).thenReturn(visualBounds.getMaxY() + 1);
    when(mainWindowPrefs.getX()).thenReturn(visualBounds.getMaxX() + 1);

    doAnswer(invocation -> {
      getRoot().getChildren().setAll((Region) invocation.getArgument(1));
      return null;
    }).when(windowController).configure(any(), any(Region.class), anyBoolean(), anyVararg());

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      instance.display();
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));

    verify(windowController).configure(any(), any(Region.class), anyBoolean(), anyVararg());

    Window window = instance.getRoot().getScene().getWindow();
    Rectangle2D bounds = new Rectangle2D(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    assertTrue(Screen.getPrimary().getBounds().contains(bounds));
  }

  @Test
  public void testOnRevealMapFolder() throws Exception {
    Path expectedPath = Paths.get("C:\\test\\path_map");
    when(forgedAlliancePrefs.getCustomMapsDirectory()).thenReturn(expectedPath);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    instance.onRevealMapFolder();
    verify(platformService).reveal(expectedPath);
  }

  @Test
  public void testOnRevealModFolder() throws Exception {
    Path expectedPath = Paths.get("C:\\test\\path_mod");
    when(forgedAlliancePrefs.getModsDirectory()).thenReturn(expectedPath);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    instance.onRevealModFolder();
    verify(platformService).reveal(expectedPath);
  }

  @Test
  public void testOnRevealLogFolder() throws Exception {
    Path expectedPath = Paths.get("C:\\test\\path_log");
    when(preferencesService.getFafLogDirectory()).thenReturn(expectedPath);
    instance.onRevealLogFolder();
    verify(platformService).reveal(expectedPath);
  }
}
