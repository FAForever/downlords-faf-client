package com.faforever.client.main;

import com.faforever.client.cast.CastsController;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.fx.WindowController;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesController;
import com.faforever.client.gravatar.GravatarService;
import com.faforever.client.hub.CommunityHubController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardController;
import com.faforever.client.login.LoginController;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.news.NewsController;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.notification.TransientNotificationsController;
import com.faforever.client.patch.GameUpdateService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.SettingsController;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.remote.FafService;
import com.faforever.client.replay.ReplayVaultController;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.UserService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.layout.Pane;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
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
  private ConnectivityService connectivityService;
  @Mock
  private GameUpdateService gameUpdateService;
  @Mock
  private PlayerService playerService;
  @Mock
  private CommunityHubController communityHubController;
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
  private GravatarService gravatarService;
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

  private MainController instance;
  private CountDownLatch mainControllerInitializedLatch;
  private SimpleObjectProperty<ConnectionState> connectionStateProperty;
  private SimpleObjectProperty<ConnectivityState> connectivityStateProperty;
  private ObjectProperty<ConnectionState> chatConnectionStateProperty;
  private BooleanProperty loggedInProperty;

  @Before
  public void setUp() throws Exception {
    instance = loadController("main.fxml");
    instance.stage = getStage();
    instance.i18n = i18n;
    instance.applicationContext = applicationContext;
    instance.playerService = playerService;
    instance.preferencesService = preferencesService;
    instance.connectivityService = connectivityService;
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
    instance.communityHubController = communityHubController;
    instance.settingsController = settingsController;
    instance.chatController = chatController;
    instance.persistentNotificationsController = persistentNotificationsController;
    instance.notificationService = notificationService;
    instance.taskService = taskService;
    instance.clientUpdateService = clientUpdateService;
    instance.gameService = gameService;
    instance.userMenuController = userMenuController;
    instance.gravatarService = gravatarService;
    instance.transientNotificationsController = transientNotificationsController;
    instance.loginController = loginController;
    instance.chatService = chatService;
    instance.threadPoolExecutor = threadPoolExecutor;
    instance.windowController = windowController;

    connectionStateProperty = new SimpleObjectProperty<>();
    connectivityStateProperty = new SimpleObjectProperty<>(ConnectivityState.UNKNOWN);
    chatConnectionStateProperty = new SimpleObjectProperty<>();
    loggedInProperty = new SimpleBooleanProperty();

    when(chatController.getRoot()).thenReturn(new Pane());
    when(persistentNotificationsController.getRoot()).thenReturn(new Pane());
    when(leaderboardController.getRoot()).thenReturn(new Pane());
    when(castsController.getRoot()).thenReturn(new Pane());
    when(userMenuController.getRoot()).thenReturn(new Pane());
    when(newsController.getRoot()).thenReturn(new Pane());
    when(communityHubController.getRoot()).thenReturn(new Pane());
    when(userMenuController.getRoot()).thenReturn(new Pane());
    when(transientNotificationsController.getRoot()).thenReturn(new Pane());
    when(taskService.getActiveTasks()).thenReturn(FXCollections.emptyObservableList());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(applicationContext.getBean(UserInfoWindowController.class)).thenReturn(userInfoWindowController);
    when(applicationContext.getBean(WindowController.class)).thenReturn(windowController);
    when(preferences.getMainWindow()).thenReturn(mainWindowPrefs);
    when(mainWindowPrefs.getLastChildViews()).thenReturn(FXCollections.observableHashMap());
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(preferences.getNotification()).thenReturn(notificationPrefs);
    when(notificationPrefs.toastPositionProperty()).thenReturn(new SimpleObjectProperty<>(ToastPosition.BOTTOM_RIGHT));
    when(notificationPrefs.getToastPosition()).thenReturn(ToastPosition.BOTTOM_RIGHT);
    when(fafService.connectionStateProperty()).thenReturn(connectionStateProperty);
    when(connectivityService.checkConnectivity()).thenReturn(CompletableFuture.completedFuture(null));
    when(connectivityService.connectivityStateProperty()).thenReturn(connectivityStateProperty);
    when(chatService.connectionStateProperty()).thenReturn(chatConnectionStateProperty);
    when(userService.loggedInProperty()).thenReturn(loggedInProperty);

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

    when(communityHubController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.display());
    when(mainWindowPrefs.getLastView()).thenReturn(instance.communityButton.getId());

    verify(gameUpdateService).checkForUpdateInBackground();
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
  public void testOnPortCheckRetryClicked() throws Exception {
    instance.onPortCheckRetryClicked();

    verify(connectivityService).checkConnectivity();
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
  public void testOnGamePortCheckFailed() throws Exception {
    testChangeConnectivity(ConnectivityState.PUBLIC, ConnectivityState.UNKNOWN);
  }

  private void testChangeConnectivity(ConnectivityState initialState, ConnectivityState newState) throws Exception {
    connectivityStateProperty.setValue(initialState);

    String unknown = "Unknown";
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.portCheckStatusButton.setText(unknown));

    CompletableFuture<String> textFuture = new CompletableFuture<>();
    instance.portCheckStatusButton.textProperty().addListener((observable, oldValue, newValue) -> {
      textFuture.complete(newValue);
    });

    connectivityStateProperty.setValue(newState);

    assertThat(textFuture.get(1, TimeUnit.SECONDS), not(unknown));
  }

  @Test
  public void testOnGamePortCheckResultProxy() throws Exception {
    testChangeConnectivity(ConnectivityState.UNKNOWN, ConnectivityState.STUN);
  }

  @Test
  public void testOnGamePortCheckResultUnreachable() throws Exception {
    testChangeConnectivity(ConnectivityState.UNKNOWN, ConnectivityState.BLOCKED);
  }

  @Test
  public void testOnGamePortCheckResultReachable() throws Exception {
    testChangeConnectivity(ConnectivityState.UNKNOWN, ConnectivityState.PUBLIC);
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
  public void testOnCommunitySelected() throws Exception {
    attachToRoot();
    when(communityHubController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.communityButton::fire);
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
  @Ignore("CommunityHub is not yet available")
  public void testOnCommunityHubSelected() throws Exception {
    when(communityHubController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.communityButton.getItems().get(0).fire());
  }

  @Test
  public void testOnNewsSelected() throws Exception {
    attachToRoot();
    when(newsController.getRoot()).thenReturn(new Pane());
    // TODO when community hub is added, this index needs to be 1
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.communityButton.getItems().get(0).fire());
  }

  @Test
  public void testOnCastsSelected() throws Exception {
    attachToRoot();
    when(castsController.getRoot()).thenReturn(new Pane());
    // TODO when community hub is added, this index needs to be 2
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.communityButton.getItems().get(1).fire());
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
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.vaultButton.getItems().get(1).fire());
  }

  @Test
  public void testOnLeaderboardRanked1v1Selected() throws Exception {
    attachToRoot();
    when(leaderboardController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.leaderboardButton.getItems().get(0).fire());
  }
}
