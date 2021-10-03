package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.event.ChatUserColorChangeEvent;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.google.common.eventbus.EventBus;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.player.SocialStatus.SELF;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserContextMenuControllerTest extends UITest {
  private static final String TEST_USER_NAME = "junit";

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private ReplayService replayService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private AvatarService avatarService;
  @Mock
  private ModeratorService moderatorService;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private ReportDialogController reportDialogController;

  private ChatUserContextMenuController instance;
  private PlayerBean player;
  private ChatChannelUser chatUser;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new ChatUserContextMenuController(playerService, eventBus, uiService,
        preferencesService, replayService, notificationService, i18n, joinGameHelper,
        avatarService, moderatorService, teamMatchmakingService);

    Preferences preferences = PreferencesBuilder.create().defaultValues().get();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(avatarService.getAvailableAvatars()).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        AvatarBeanBuilder.create().defaultValues().url(new URL("http://www.example.com/avatar1.png")).description("Avatar Number #1").get(),
        AvatarBeanBuilder.create().defaultValues().url(new URL("http://www.example.com/avatar2.png")).description("Avatar Number #2").get(),
        AvatarBeanBuilder.create().defaultValues().url(new URL("http://www.example.com/avatar3.png")).description("Avatar Number #3").get()
    )));
    when(moderatorService.getPermissions()).thenReturn(Collections.emptySet());
    when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);

    loadFxml("theme/player_context_menu.fxml", clazz -> instance, instance);

    player = PlayerBeanBuilder.create().defaultValues().username(TEST_USER_NAME).socialStatus(SELF).avatar(null).get();
    chatUser = ChatChannelUserBuilder.create(TEST_USER_NAME).defaultValues().player(player).get();

    runOnFxThreadAndWait(() -> instance.setChatUser(chatUser));
  }

  @Test
  public void testShowCorrectItems() {
    assertThat(instance.showUserInfo.isVisible(), is(true));
    assertThat(instance.sendPrivateMessageItem.isVisible(), is(false));
    assertThat(instance.copyUsernameItem.isVisible(), is(true));
    assertThat(instance.colorPickerMenuItem.isVisible(), is(true));
    assertThat(instance.inviteItem.isVisible(), is(false));
    assertThat(instance.addFriendItem.isVisible(), is(false));
    assertThat(instance.removeFriendItem.isVisible(), is(false));
    assertThat(instance.addFoeItem.isVisible(), is(false));
    assertThat(instance.removeFoeItem.isVisible(), is(false));
    assertThat(instance.reportItem.isVisible(), is(false));
    assertThat(instance.joinGameItem.isVisible(), is(false));
    assertThat(instance.watchGameItem.isVisible(), is(false));
    assertThat(instance.viewReplaysItem.isVisible(), is(true));
    assertThat(instance.kickGameItem.isVisible(), is(false));
    assertThat(instance.kickLobbyItem.isVisible(), is(false));
    assertThat(instance.broadcastMessage.isVisible(), is(false));
    assertThat(instance.avatarPickerMenuItem.isVisible(), is(true));
  }

  @Test
  public void testShowCorrectItemsForOther() {
    player.setSocialStatus(OTHER);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.showUserInfo.isVisible(), is(true));
    assertThat(instance.sendPrivateMessageItem.isVisible(), is(true));
    assertThat(instance.copyUsernameItem.isVisible(), is(true));
    assertThat(instance.colorPickerMenuItem.isVisible(), is(true));
    assertThat(instance.inviteItem.isVisible(), is(true));
    assertThat(instance.addFriendItem.isVisible(), is(true));
    assertThat(instance.removeFriendItem.isVisible(), is(false));
    assertThat(instance.addFoeItem.isVisible(), is(true));
    assertThat(instance.removeFoeItem.isVisible(), is(false));
    assertThat(instance.reportItem.isVisible(), is(true));
    assertThat(instance.joinGameItem.isVisible(), is(false));
    assertThat(instance.watchGameItem.isVisible(), is(false));
    assertThat(instance.viewReplaysItem.isVisible(), is(true));
    assertThat(instance.kickGameItem.isVisible(), is(false));
    assertThat(instance.kickLobbyItem.isVisible(), is(false));
    assertThat(instance.broadcastMessage.isVisible(), is(false));
    assertThat(instance.avatarPickerMenuItem.isVisible(), is(false));
  }

  @Test
  public void testJoinGameContextMenuNotShownForIdleUser() {
    player.setSocialStatus(OTHER);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.joinGameItem.isVisible());
  }

  @Test
  public void testJoinGameContextMenuShownForHostingUser() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).host(player.getUsername()).get();

    player.setSocialStatus(OTHER);
    player.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(player.getStatus(), PlayerStatus.HOSTING);
    assertTrue(instance.joinGameItem.isVisible());
  }

  @Test
  public void testJoinGameContextMenuNotShownForMatchmakerPlayer() {
    GameBean game = GameBeanBuilder.create().defaultValues().gameType(GameType.MATCHMAKER).status(GameStatus.OPEN).host(player.getUsername()).get();

    player.setSocialStatus(OTHER);
    player.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(player.getStatus(), PlayerStatus.HOSTING);
    assertFalse(instance.joinGameItem.isVisible());
  }

  @Test
  public void testInviteContextMenuShownForIdleUser() {
    player.setGame(null);
    player.setSocialStatus(OTHER);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.inviteItem.isVisible());
  }

  @Test
  public void testInviteContextMenuNotShownForHostingUser() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).host(player.getUsername()).get();

    player.setSocialStatus(OTHER);
    player.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(player.getStatus(), PlayerStatus.HOSTING);
    assertFalse(instance.inviteItem.isVisible());
  }

  @Test
  public void testInviteContextMenuNotShownForLobbyingUser() {
    GameBean game = GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).host("otherPlayer").get();

    player.setSocialStatus(OTHER);
    player.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(player.getStatus(), PlayerStatus.LOBBYING);
    assertFalse(instance.inviteItem.isVisible());
  }

  @Test
  public void testInviteContextMenuNotShownForPlayingUser() {
    GameBean game = new GameBean();
    game.setFeaturedMod(KnownFeaturedMod.FAF.getTechnicalName());
    game.setStatus(GameStatus.PLAYING);
    game.setHost(player.getUsername());

    player.setSocialStatus(OTHER);
    player.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(player.getStatus(), PlayerStatus.PLAYING);
    assertFalse(instance.inviteItem.isVisible());
  }

  @Test
  public void testOnSendPrivateMessage() {
    instance.onSendPrivateMessageSelected();

    verify(eventBus).post(any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testChangePlayerColor() {
    instance.colorPicker.setValue(Color.ALICEBLUE);
    WaitForAsyncUtils.waitForFxEvents();

    verify(eventBus).post(any(ChatUserColorChangeEvent.class));
  }

  @Test
  public void testOnInvite() {
    instance.onInviteToGameSelected();

    verify(teamMatchmakingService).invitePlayer(TEST_USER_NAME);
  }

  @Test
  public void testOnSelectAvatar() throws Exception {
    instance.avatarComboBox.show();

    WaitForAsyncUtils.waitForAsyncFx(100_000, () -> instance.avatarComboBox.getSelectionModel().select(2));

    ArgumentCaptor<AvatarBean> captor = ArgumentCaptor.forClass(AvatarBean.class);
    verify(avatarService).changeAvatar(captor.capture());

    AvatarBean avatarBean = captor.getValue();
    assertThat(avatarBean.getUrl(), equalTo(new URL("http://www.example.com/avatar2.png")));
    assertThat(avatarBean.getDescription(), is("Avatar Number #2"));
  }

  @Test
  public void testHideItemsIfNoPlayer() throws Exception {
    // We can't just set the player to null and this would also never happen
    // in a real scenario, so we need a new object.

    instance = new ChatUserContextMenuController(playerService, eventBus, uiService,
        preferencesService, replayService, notificationService, i18n, joinGameHelper,
        avatarService, moderatorService, teamMatchmakingService);
    loadFxml("theme/player_context_menu.fxml", clazz -> instance, instance);

    chatUser = ChatChannelUserBuilder.create(TEST_USER_NAME).defaultValues().get();
    runOnFxThreadAndWait(() -> instance.setChatUser(chatUser));

    assertThat(instance.showUserInfo.isVisible(), is(false));
    assertThat(instance.sendPrivateMessageItem.isVisible(), is(true));
    assertThat(instance.copyUsernameItem.isVisible(), is(true));
    assertThat(instance.colorPickerMenuItem.isVisible(), is(true));
    assertThat(instance.inviteItem.isVisible(), is(false));
    assertThat(instance.addFriendItem.isVisible(), is(false));
    assertThat(instance.removeFriendItem.isVisible(), is(false));
    assertThat(instance.addFoeItem.isVisible(), is(false));
    assertThat(instance.removeFoeItem.isVisible(), is(false));
    assertThat(instance.reportItem.isVisible(), is(false));
    assertThat(instance.joinGameItem.isVisible(), is(false));
    assertThat(instance.watchGameItem.isVisible(), is(false));
    assertThat(instance.viewReplaysItem.isVisible(), is(false));
    assertThat(instance.kickGameItem.isVisible(), is(false));
    assertThat(instance.kickLobbyItem.isVisible(), is(false));
    assertThat(instance.broadcastMessage.isVisible(), is(false));
    assertThat(instance.avatarPickerMenuItem.isVisible(), is(false));
  }
}
