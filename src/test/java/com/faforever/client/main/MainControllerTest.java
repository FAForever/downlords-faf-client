package com.faforever.client.main;

import ch.micheljung.fxborderlessscene.borderless.BorderlessScene;
import com.faforever.client.chat.ChatController;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GamePathHandler;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
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
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.preferences.ui.SettingsController;
import com.faforever.client.rankedmatch.MatchmakerMessage;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
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
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainControllerTest extends AbstractPlainJavaFxTest {

  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
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
  private Preferences preferences;
  @Mock
  private I18n i18n;
  @Mock
  private WindowPrefs mainWindowPrefs;
  @Mock
  private NotificationService notificationService;
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
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private GamePathHandler gamePathHandler;
  @Mock
  private ChatController chatController;
  private MainController instance;
  private BooleanProperty gameRunningProperty;

  @Override
  protected boolean showStage() {
    // Don't show the stage yet as it will be done by MainController.display()
    return false;
  }

  @Before
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getTrueSkill()
        .setBeta(240)
        .setInitialMean(1500)
        .setInitialStandardDeviation(500);

    instance = new MainController(preferencesService, i18n, notificationService, playerService, gameService, clientUpdateService,
        uiService, eventBus, clientProperties, gamePathHandler, platformService);

    gameRunningProperty = new SimpleBooleanProperty();
    ObjectProperty<Path> backgroundImagePathProperty = new SimpleObjectProperty<>();

    when(persistentNotificationsController.getRoot()).thenReturn(new Pane());
    when(transientNotificationsController.getRoot()).thenReturn(new Pane());
    when(loginController.getRoot()).thenReturn(new Pane());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.createScene(any(), any())).thenAnswer(invocation -> {
      Stage stage = invocation.getArgument(0);
      Parent root = invocation.getArgument(1);
      return new BorderlessScene(stage, root, 0, 0);
    });
    when(preferences.getMainWindow()).thenReturn(mainWindowPrefs);
    when(preferences.getNotification()).thenReturn(notificationPrefs);
    when(preferences.getMainWindow().backgroundImagePathProperty()).thenReturn(backgroundImagePathProperty);
    when(gameService.gameRunningProperty()).thenReturn(gameRunningProperty);
    when(uiService.getThemeFile("theme/images/login-background.jpg")).thenReturn(getClass().getResource("/theme/images/login-background.jpg").toString());
    when(uiService.loadFxml("theme/persistent_notifications.fxml")).thenReturn(persistentNotificationsController);
    when(uiService.loadFxml("theme/transient_notifications.fxml")).thenReturn(transientNotificationsController);
    when(uiService.loadFxml("theme/settings/settings.fxml")).thenReturn(settingsController);
    when(uiService.loadFxml("theme/login.fxml")).thenReturn(loginController);
    when(uiService.loadFxml("theme/chat/chat.fxml")).thenReturn(chatController);

    loadFxml("theme/main.fxml", clazz -> {
      if (clazz == instance.getClass()) {
        return instance;
      }
      return mock(clazz);
    });
    WaitForAsyncUtils.asyncFx(() -> instance.display());
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void testHideNotifications() throws Exception {
    Platform.runLater(() -> instance.new ToastDisplayer(transientNotificationsController).invalidated(mock(SimpleBooleanProperty.class)));
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
    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().add(instance.mainRoot));
    WaitForAsyncUtils.waitForFxEvents();
  }

  private void fakeLogin() throws InterruptedException {
    instance.onLoginSuccessEvent(new LoginSuccessEvent("junit", "", 1));
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void testOnNotificationsButtonClicked() throws Exception {
    fakeLogin();
    WaitForAsyncUtils.asyncFx(instance::onNotificationsButtonClicked);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.persistentNotificationsPopup.isShowing(), is(true));
  }

  @Test
  public void testOnSettingsItemSelected() throws Exception {
    fakeLogin();

    Pane root = new Pane();
    when(settingsController.getRoot()).thenReturn(root);
    WaitForAsyncUtils.waitForAsyncFx(1000, instance::onSettingsSelected);

    verify(uiService).createScene(any(), eq(root));
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

  @Test
  public void testOnChat() throws Exception {
    instance.chatButton.pseudoClassStateChanged(HIGHLIGHTED, true);
    instance.onChat(new ActionEvent(instance.chatButton, Event.NULL_SOURCE_TARGET));
    assertThat(instance.chatButton.getPseudoClassStates().contains(HIGHLIGHTED), is(false));

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

    WaitForAsyncUtils.asyncFx(() -> instance.display());
    WaitForAsyncUtils.waitForFxEvents();
    fakeLogin();

    // Twice; once from setUp(), once from above
    verify(uiService, times(2)).createScene(any(), any());

    Window window = instance.getRoot().getScene().getWindow();
    Rectangle2D bounds = new Rectangle2D(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    assertTrue(Screen.getPrimary().getBounds().contains(bounds));

    // Test if maximize/restore also centers
    WaitForAsyncUtils.asyncFx(() -> {
      BorderlessScene scene = (BorderlessScene) StageHolder.getStage().getScene();
      scene.maximizeStage();
      scene.maximizeStage();
    });
    WaitForAsyncUtils.waitForFxEvents();

    Rectangle2D newBounds = new Rectangle2D(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    assertTrue(Screen.getPrimary().getBounds().contains(newBounds));
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
