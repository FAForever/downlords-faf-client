package com.faforever.client.main;

import com.faforever.client.cast.CastsController;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.WindowController;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardController;
import com.faforever.client.login.LoginController;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.news.NewsController;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.notification.TransientNotificationsController;
import com.faforever.client.patch.GameUpdateService;
import com.faforever.client.player.PlayerInfoBeanBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.ui.SettingsController;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.rankedmatch.MatchmakerMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.replay.ReplayVaultController;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
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
import org.springframework.context.ApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private PersistentNotificationsController persistentNotificationsController;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private LeaderboardController leaderboardController;
  @Mock
  private GameUpdateService gameUpdateService;
  @Mock
  private PlayerService playerService;
  @Mock
  private MapVaultController mapMapVaultController;
  @Mock
  private GamesController gamesController;
  @Mock
  private NewsController newsController;
  @Mock
  private CastsController castsController;
  @Mock
  private ModVaultController modVaultController;
  @Mock
  private ReplayVaultController replayVaultController;
  @Mock
  private ChatController chatController;
  @Mock
  private SettingsController settingsController;
  @Mock
  private UserInfoWindowController userInfoWindowController;
  @Mock
  private Preferences preferences;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private I18n i18n;
  @Mock
  private WindowPrefs mainWindowPrefs;
  @Mock
  private FafService fafService;
  @Mock
  private UserService userService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private TaskService taskService;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ClientUpdateService clientUpdateService;
  @Mock
  private GameService gameService;
  @Mock
  private UserMenuController userMenuController;
  @Mock
  private TransientNotificationsController transientNotificationsController;
  @Mock
  private NotificationsPrefs notificationPrefs;
  @Mock
  private LoginController loginController;
  @Mock
  private ChatService chatService;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;
  @Mock
  private WindowController windowController;
  @Mock
  private ThemeService themeService;
  @Mock
  private EventBus eventBus;

  private MainController instance;
  private CountDownLatch mainControllerInitializedLatch;
  private SimpleObjectProperty<ConnectionState> connectionStateProperty;
  private BooleanProperty loggedInProperty;
  private BooleanProperty gameRunningProperty;

  @Before
  public void setUp() throws Exception {
    instance = loadController("main.fxml");
    instance.stage = getStage();
    instance.i18n = i18n;
    instance.applicationContext = applicationContext;
    instance.playerService = playerService;
    instance.preferencesService = preferencesService;
    instance.gameUpdateService = gameUpdateService;
    instance.fafService = fafService;
    instance.userService = userService;
    instance.replayVaultController = replayVaultController;
    instance.leaderboardController = leaderboardController;
    instance.modVaultController = modVaultController;
    instance.mapMapVaultController = mapMapVaultController;
    instance.gamesController = gamesController;
    instance.castsController = castsController;
    instance.newsController = newsController;
    instance.settingsController = settingsController;
    instance.chatController = chatController;
    instance.persistentNotificationsController = persistentNotificationsController;
    instance.notificationService = notificationService;
    instance.taskService = taskService;
    instance.clientUpdateService = clientUpdateService;
    instance.gameService = gameService;
    instance.userMenuController = userMenuController;
    instance.transientNotificationsController = transientNotificationsController;
    instance.loginController = loginController;
    instance.chatService = chatService;
    instance.threadPoolExecutor = threadPoolExecutor;
    instance.windowController = windowController;
    instance.themeService = themeService;
    instance.eventBus = eventBus;
    instance.ratingBeta = 250;

    connectionStateProperty = new SimpleObjectProperty<>();
    ObjectProperty<ConnectionState> chatConnectionStateProperty = new SimpleObjectProperty<>();
    loggedInProperty = new SimpleBooleanProperty();
    gameRunningProperty = new SimpleBooleanProperty();
    IntegerProperty chatUnreadMessagesCountProperty = new SimpleIntegerProperty();

    when(chatController.getRoot()).thenReturn(new Pane());
    when(persistentNotificationsController.getRoot()).thenReturn(new Pane());
    when(leaderboardController.getRoot()).thenReturn(new Pane());
    when(castsController.getRoot()).thenReturn(new Pane());
    when(userMenuController.getRoot()).thenReturn(new Pane());
    when(newsController.getRoot()).thenReturn(new Pane());
    when(userMenuController.getRoot()).thenReturn(new Pane());
    when(transientNotificationsController.getRoot()).thenReturn(new Pane());
    when(taskService.getActiveTasks()).thenReturn(FXCollections.emptyObservableList());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(applicationContext.getBean(UserInfoWindowController.class)).thenReturn(userInfoWindowController);
    when(applicationContext.getBean(WindowController.class)).thenReturn(windowController);
    when(preferences.getMainWindow()).thenReturn(mainWindowPrefs);
    when(mainWindowPrefs.getLastChildViews()).thenReturn(FXCollections.observableHashMap());
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.portProperty()).thenReturn(new SimpleIntegerProperty());
    when(preferences.getNotification()).thenReturn(notificationPrefs);
    when(notificationPrefs.toastPositionProperty()).thenReturn(new SimpleObjectProperty<>(ToastPosition.BOTTOM_RIGHT));
    when(notificationPrefs.getToastPosition()).thenReturn(ToastPosition.BOTTOM_RIGHT);
    when(fafService.connectionStateProperty()).thenReturn(connectionStateProperty);
    when(chatService.connectionStateProperty()).thenReturn(chatConnectionStateProperty);
    when(chatService.unreadMessagesCount()).thenReturn(chatUnreadMessagesCountProperty);
    when(userService.loggedInProperty()).thenReturn(loggedInProperty);
    when(gameService.gameRunningProperty()).thenReturn(gameRunningProperty);

    doAnswer(invocation -> getThemeFile(invocation.getArgumentAt(0, String.class))).when(themeService).getThemeFile(any());

    instance.postConstruct();

    verify(userService).loggedInProperty();

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

    when(chatController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.display());
    when(mainWindowPrefs.getLastView()).thenReturn(instance.newsButton.getId());

    assertTrue(getStage().isShowing());
  }

  /**
   * Attaches the instance's root to the test's root. This is necessary since some components only work properly if they
   * are attached to a window.
   */
  private void attachToRoot() {
    WaitForAsyncUtils.waitForAsyncFx(5000, () -> getRoot().getChildren().add(instance.mainRoot));
    WaitForAsyncUtils.waitForFxEvents();
  }

  private void fakeLogin() throws InterruptedException {
    loggedInProperty.set(true);
    assertTrue(mainControllerInitializedLatch.await(3000, TimeUnit.SECONDS));
  }

  @Test
  public void testOnFaConnectedDoesntThrowUp() throws Exception {
    String disconnectedText = "foobar";
    instance.fafConnectionButton.setText(disconnectedText);

    CompletableFuture<String> textFuture = new CompletableFuture<>();
    instance.fafConnectionButton.textProperty().addListener((observable, oldValue, newValue) -> {
      textFuture.complete(newValue);
    });

    connectionStateProperty.set(ConnectionState.CONNECTED);

    assertThat(textFuture.get(3, TimeUnit.SECONDS), not(disconnectedText));
  }

  @Test
  public void testOnFaConnecting() throws Exception {
    String disconnectedText = "foobar";
    instance.fafConnectionButton.setText(disconnectedText);

    CompletableFuture<String> textFuture = new CompletableFuture<>();
    instance.fafConnectionButton.textProperty().addListener((observable, oldValue, newValue) -> {
      textFuture.complete(newValue);
    });

    connectionStateProperty.set(ConnectionState.CONNECTING);

    assertThat(textFuture.get(3, TimeUnit.SECONDS), not(disconnectedText));
  }

  @Test
  public void testOnFafDisconnected() throws Exception {
    String disconnectedText = "foobar";
    instance.fafConnectionButton.setText(disconnectedText);

    CompletableFuture<String> textFuture = new CompletableFuture<>();
    instance.fafConnectionButton.textProperty().addListener((observable, oldValue, newValue) -> {
      textFuture.complete(newValue);
    });

    connectionStateProperty.set(ConnectionState.DISCONNECTED);

    assertThat(textFuture.get(3, TimeUnit.SECONDS), not(disconnectedText));
  }

  @Test
  @Ignore("Not yet implemented")
  public void testOnPortCheckHelpClicked() throws Exception {
    instance.onPortCheckHelpClicked();
  }

  @Test
  @Ignore("Not yet implemented")
  public void testOnChangePortClicked() throws Exception {
    instance.onChangePortClicked();
  }

  @Test
  public void testOnFafReconnectClicked() throws Exception {
    instance.onFafReconnectClicked();
    verify(fafService).reconnect();
  }

  @Test
  public void testOnIrcReconnectClicked() throws Exception {
    instance.onChatReconnectClicked();
    verify(chatService).reconnect();
  }

  @Test
  public void testOnNotificationsButtonClicked() throws Exception {
    attachToRoot();
    WaitForAsyncUtils.waitForAsyncFx(1000, instance::onNotificationsButtonClicked);

    assertThat(instance.persistentNotificationsPopup.isShowing(), is(true));
  }

  @Test
  @Ignore("Not yet implemented")
  public void testOnSupportItemSelected() throws Exception {
    instance.onSupportItemSelected();
  }

  @Test
  public void testOnSettingsItemSelected() throws Exception {
    attachToRoot();
    Pane root = new Pane();
    when(settingsController.getRoot()).thenReturn(root);
    WaitForAsyncUtils.waitForAsyncFx(1000, instance::onSettingsItemSelected);

    verify(windowController).configure(
        any(), eq(root), eq(true), eq(WindowController.WindowButtonType.CLOSE)
    );
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
  public void testOnVaultSelected() throws Exception {
    attachToRoot();
    when(modVaultController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.vaultButton::fire);
  }

  @Test
  public void testOnLeaderboardSelected() throws Exception {
    attachToRoot();
    when(leaderboardController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.leaderboardButton::fire);
  }

  @Test
  public void testOnPlaySelected() throws Exception {
    attachToRoot();
    when(gamesController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.playButton::fire);
  }

  @Test
  public void testOnChatSelected() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.chatButton::fire);
  }

  @Test
  public void testOnNewsSelected() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.newsButton::fire);
  }

  @Test
  public void testOnPlayCustomSelected() throws Exception {
    attachToRoot();
    when(gamesController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.playButton.getItems().get(0).fire());
  }

  @Test
  @Ignore("Not yet implemented")
  public void testOnPlayRanked1v1Selected() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.playButton.getItems().get(1).fire());
  }

  @Test
  public void testOnModsSelected() throws Exception {
    attachToRoot();
    when(modVaultController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.vaultButton.getItems().get(0).fire());
  }

  @Test
  public void testOnReplaysSelected() throws Exception {
    attachToRoot();
    when(replayVaultController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.vaultButton.getItems().get(2).fire());
  }

  @Test
  public void testOnLeaderboardRanked1v1Selected() throws Exception {
    attachToRoot();
    when(leaderboardController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.leaderboardButton.getItems().get(0).fire());
  }

  @Test
  public void testOnMatchMakerMessageDisplaysNotification80Quality() {
    prepareTestMatchmakerMessageTest(100);
    verify(notificationService).addNotification(any(TransientNotification.class));
  }

  private void prepareTestMatchmakerMessageTest(float deviation) {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Consumer<MatchmakerMessage>> matchmakerMessageCaptor = ArgumentCaptor.forClass(Consumer.class);
    when(notificationPrefs.isRanked1v1ToastEnabled()).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(
        PlayerInfoBeanBuilder.create("JUnit").leaderboardRatingMean(1500).leaderboardRatingDeviation(deviation).get()
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
    when(notificationPrefs.isRanked1v1ToastEnabled()).thenReturn(false);
    when(playerService.getCurrentPlayer()).thenReturn(
        PlayerInfoBeanBuilder.create("JUnit").leaderboardRatingMean(1500).leaderboardRatingDeviation(100).get()
    );

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
      getRoot().getChildren().setAll(invocation.getArgumentAt(1, Region.class));
      return null;
    }).when(windowController).configure(any(), any(), anyBoolean(), anyVararg());

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      instance.display();
      latch.countDown();
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS));

    verify(windowController).configure(any(), any(), anyBoolean(), anyVararg());

    Window window = instance.getRoot().getScene().getWindow();
    Rectangle2D bounds = new Rectangle2D(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    assertTrue(Screen.getPrimary().getBounds().contains(bounds));
  }
}
