package com.faforever.client.main;

import com.faforever.client.cast.CastsController;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.WindowDecorator;
import com.faforever.client.game.GamesController;
import com.faforever.client.hub.CommunityHubController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardController;
import com.faforever.client.lobby.LobbyService;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.news.NewsController;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.patch.PatchService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.portcheck.PortCheckService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.SettingsController;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.replay.ReplayVaultController;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import javafx.collections.FXCollections;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  PersistentNotificationsController persistentNotificationsController;
  @Mock
  PreferencesService preferencesService;
  @Mock
  LeaderboardController leaderboardController;
  @Mock
  ChatService chatService;
  @Mock
  SceneFactory sceneFactory;
  @Mock
  PortCheckService portCheckService;
  @Mock
  PatchService patchService;
  @Mock
  PlayerService playerService;
  @Mock
  CommunityHubController communityHubController;
  @Mock
  MapVaultController mapMapVaultController;
  @Mock
  GamesController gamesController;
  @Mock
  NewsController newsController;
  @Mock
  CastsController castsController;
  @Mock
  ModVaultController modVaultController;
  @Mock
  ReplayVaultController replayVaultController;
  @Mock
  ChatController chatController;
  @Mock
  SettingsController settingsController;
  @Mock
  UserInfoWindowController userInfoWindowController;
  @Mock
  Preferences preferences;
  @Mock
  ApplicationContext applicationContext;
  @Mock
  I18n i18n;
  @Mock
  WindowPrefs mainWindowPrefs;
  @Mock
  LobbyService lobbyService;
  @Mock
  Environment environment;
  @Mock
  UserService userService;
  @Mock
  NotificationService notificationService;
  @Mock
  TaskService taskService;
  @Mock
  ForgedAlliancePrefs forgedAlliancePrefs;
  private MainController instance;

  @Override
  public void start(Stage stage) throws Exception {
    super.start(stage);

    instance = loadController("main.fxml");
    instance.i18n = i18n;
    instance.environment = environment;
    instance.applicationContext = applicationContext;
    instance.playerService = playerService;
    instance.sceneFactory = sceneFactory;
    instance.preferencesService = preferencesService;
    instance.portCheckService = portCheckService;
    instance.patchService = patchService;
    instance.lobbyService = lobbyService;
    instance.chatService = chatService;
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

    when(persistentNotificationsController.getRoot()).thenReturn(new Pane());
    when(leaderboardController.getRoot()).thenReturn(new Pane());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(applicationContext.getBean(UserInfoWindowController.class)).thenReturn(userInfoWindowController);
    when(preferences.getMainWindow()).thenReturn(mainWindowPrefs);
    when(mainWindowPrefs.getLastChildViews()).thenReturn(FXCollections.observableHashMap());
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);

    getRoot().getChildren().setAll(instance.getRoot());

    instance.postConstruct();
  }

  @Test
  public void testDisplay() throws Exception {
    when(communityHubController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.display(getStage()));
    when(mainWindowPrefs.getLastView()).thenReturn(instance.communityButton.getId());

    verify(chatService).connect();
    verify(portCheckService).checkGamePortInBackground();
    verify(patchService).checkForUpdatesInBackground();
    verify(lobbyService).setOnFafConnectedListener(instance);
    verify(lobbyService).setOnLobbyConnectingListener(instance);
    verify(lobbyService).setOnFafDisconnectedListener(instance);
  }

  @Test
  public void testOnFaConnectedDoesntThrowUp() throws Exception {
    String disconnected = "foobar";
    instance.fafConnectionButton.setText(disconnected);

    instance.onFaConnected();

    String textAfterConnection = instance.fafConnectionButton.getText();
    assertThat(textAfterConnection, not(disconnected));
  }

  @Test
  public void testOnFaConnecting() throws Exception {
    String disconnected = "foobar";
    instance.fafConnectionButton.setText(disconnected);

    instance.onFaConnecting();

    String textAfterConnection = instance.fafConnectionButton.getText();
    assertThat(textAfterConnection, not(disconnected));
  }

  @Test
  public void testOnFafDisconnected() throws Exception {
    String disconnected = "foobar";
    instance.fafConnectionButton.setText(disconnected);

    instance.onFafDisconnected();

    String textAfterConnection = instance.fafConnectionButton.getText();
    assertThat(textAfterConnection, not(disconnected));
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
  @Ignore("Not yet implemented")
  public void testOnEnableUpnpClicked() throws Exception {
    instance.onEnableUpnpClicked();
  }

  @Test
  public void testOnPortCheckRetryClicked() throws Exception {
    instance.onPortCheckRetryClicked();

    verify(portCheckService).checkGamePortInBackground();
  }

  @Test
  @Ignore("Not yet implemented")
  public void testOnFafReconnectClicked() throws Exception {
    instance.onFafReconnectClicked();
  }

  @Test
  @Ignore("Not yet implemented")
  public void testOnIrcReconnectClicked() throws Exception {
    instance.onIrcReconnectClicked();
  }

  @Test
  public void testOnNotificationsButtonClicked() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(1000, instance::onNotificationsButtonClicked);

    assertThat(instance.notificationsPopup.isShowing(), is(true));
  }

  @Test
  public void testOnGamePortCheckResult() throws Exception {
    String disconnected = "foobar";
    instance.portCheckStatusButton.setText(disconnected);

    instance.onGamePortCheckResult(false);

    String textAfterConnection = instance.portCheckStatusButton.getText();
    assertThat(textAfterConnection, not(disconnected));
  }

  @Test
  public void testOnGamePortCheckStarted() throws Exception {
    String disconnected = "foobar";
    instance.portCheckStatusButton.setText(disconnected);

    instance.onGamePortCheckStarted();

    String textAfterConnection = instance.portCheckStatusButton.getText();
    assertThat(textAfterConnection, not(disconnected));
  }

  @Test
  @Ignore("Not yet implemented")
  public void testOnSupportItemSelected() throws Exception {
    instance.onSupportItemSelected();
  }

  @Test
  public void testOnSettingsItemSelected() throws Exception {
    Pane root = new Pane();
    when(settingsController.getRoot()).thenReturn(root);
    WaitForAsyncUtils.waitForAsyncFx(1000, instance::onSettingsItemSelected);

    verify(sceneFactory).createScene(
        any(), eq(root), eq(true), eq(WindowDecorator.WindowButtonType.CLOSE)
    );
  }

  @Test
  @Ignore("Needs UI for testing")
  public void testOnChoseGameDirectory() throws Exception {
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), instanceOf(Pane.class));
  }

  @Test
  public void testOnShowUserInfoClicked() throws Exception {
    Pane root = new Pane();
    when(userInfoWindowController.getRoot()).thenReturn(root);
    WaitForAsyncUtils.waitForAsyncFx(1000, instance::onShowUserInfoClicked);

    verify(sceneFactory).createScene(
        any(), eq(root), eq(true), eq(WindowDecorator.WindowButtonType.CLOSE)
    );
    verify(playerService).getCurrentPlayer();
  }

  @Test
  public void testOnCommunitySelected() throws Exception {
    when(communityHubController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.communityButton::fire);
  }

  @Test
  public void testOnVaultSelected() throws Exception {
    when(mapMapVaultController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.vaultButton::fire);
  }

  @Test
  public void testOnLeaderboardSelected() throws Exception {
    when(leaderboardController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.leaderboardButton::fire);
  }

  @Test
  public void testOnPlaySelected() throws Exception {
    when(gamesController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.playButton::fire);
  }

  @Test
  public void testOnChatSelected() throws Exception {
    when(chatController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, instance.chatButton::fire);
  }

  @Test
  public void testOnCommunityHubSelected() throws Exception {
    when(communityHubController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.communityButton.getItems().get(0).fire());
  }

  @Test
  public void testOnNewsSelected() throws Exception {
    when(newsController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.communityButton.getItems().get(1).fire());
  }

  @Test
  public void testOnCastsSelected() throws Exception {
    when(castsController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.communityButton.getItems().get(2).fire());
  }

  @Test
  public void testOnPlayCustomSelected() throws Exception {
    when(gamesController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.playButton.getItems().get(0).fire());
  }

  @Test
  @Ignore("Not yet implemented")
  public void testOnPlayRanked1v1Selected() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.playButton.getItems().get(1).fire());
  }

  @Test
  public void testOnMapsSelected() throws Exception {
    when(mapMapVaultController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.vaultButton.getItems().get(0).fire());
  }

  @Test
  public void testOnModsSelected() throws Exception {
    when(modVaultController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.vaultButton.getItems().get(1).fire());
  }

  @Test
  public void testOnReplaysSelected() throws Exception {
    when(replayVaultController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.vaultButton.getItems().get(2).fire());
  }

  @Test
  public void testOnLeaderboardRanked1v1Selected() throws Exception {
    when(leaderboardController.getRoot()).thenReturn(new Pane());
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.leaderboardButton.getItems().get(0).fire());
  }
}
