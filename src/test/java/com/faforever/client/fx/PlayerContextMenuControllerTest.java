package com.faforever.client.fx;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.api.dto.GroupPermission;
import com.faforever.commons.lobby.GameStatus;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.player.SocialStatus.SELF;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerContextMenuControllerTest extends UITest {
  private static final String TEST_USER_NAME = "junit";

  @Mock
  private AvatarService avatarService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private ModeratorService moderatorService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private PlayerService playerService;
  @Mock
  private ReplayService replayService;
  @Mock
  private UiService uiService;
  @Mock
  private ReportDialogController reportDialogController;

  private PlayerContextMenuController instance;
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    when(avatarService.getAvailableAvatars()).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        AvatarBeanBuilder.create().defaultValues().url(new URL("http://www.example.com/avatar1.png")).description("Avatar Number #1").get(),
        AvatarBeanBuilder.create().defaultValues().url(new URL("http://www.example.com/avatar2.png")).description("Avatar Number #2").get(),
        AvatarBeanBuilder.create().defaultValues().url(new URL("http://www.example.com/avatar3.png")).description("Avatar Number #3").get()
    )));
    when(moderatorService.getPermissions()).thenReturn(Collections.emptySet());
    when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);

    instance = new PlayerContextMenuController(avatarService, eventBus, i18n, joinGameHelper, moderatorService,
        notificationService, playerService, replayService, uiService) {};
    loadFxml("theme/player_context_menu.fxml", clazz -> instance, instance);

    player = PlayerBeanBuilder.create().defaultValues().username(TEST_USER_NAME).socialStatus(SELF).avatar(null).game(new GameBean()).get();
    instance.setPlayer(player);
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void testShowCorrectItems() {
    assertThat(instance.showUserInfo.isVisible(), is(true));
    assertThat(instance.sendPrivateMessageItem.isVisible(), is(false));
    assertThat(instance.copyUsernameItem.isVisible(), is(true));
    assertThat(instance.colorPickerMenuItem.isVisible(), is(false));
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
    assertThat(instance.sendPrivateMessageItem.isVisible(), is(false));
    assertThat(instance.copyUsernameItem.isVisible(), is(true));
    assertThat(instance.colorPickerMenuItem.isVisible(), is(false));
    assertThat(instance.inviteItem.isVisible(), is(false));
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
  public void testOnAddFriendWithFoe() {
    player.setSocialStatus(FOE);

    WaitForAsyncUtils.waitForFxEvents();

    instance.onAddFriendSelected();

    verify(playerService).removeFoe(player);
    verify(playerService).addFriend(player);
  }

  @Test
  public void testOnAddFriendWithNeutral() {
    player.setSocialStatus(OTHER);

    WaitForAsyncUtils.waitForFxEvents();

    instance.onAddFriendSelected();

    verify(playerService, never()).removeFoe(player);
    verify(playerService).addFriend(player);
  }

  @Test
  public void testOnRemoveFriend() {
    instance.onRemoveFriendSelected();

    verify(playerService).removeFriend(player);
  }

  @Test
  public void testOnAddFoeWithFriend() {
    player.setSocialStatus(FRIEND);

    WaitForAsyncUtils.waitForFxEvents();

    instance.onAddFoeSelected();

    verify(playerService).removeFriend(player);
    verify(playerService).addFoe(player);
  }

  @Test
  public void testOnAddFoeWithNeutral() {
    player.setSocialStatus(OTHER);

    WaitForAsyncUtils.waitForFxEvents();

    instance.onAddFoeSelected();

    verify(playerService, never()).removeFriend(player);
    verify(playerService).addFoe(player);
  }

  @Test
  public void testOnRemoveFoe() {
    instance.onRemoveFoeSelected();

    verify(playerService).removeFoe(player);
  }

  @Test
  public void testRemoveFoeShownForFoe() {
    player.setSocialStatus(FOE);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.removeFoeItem.isVisible());
    assertFalse(instance.addFoeItem.isVisible());
  }

  @Test
  public void testRemoveFriendShownForFriend() {
    player.setSocialStatus(FRIEND);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.removeFriendItem.isVisible());
    assertFalse(instance.addFriendItem.isVisible());
  }

  @Test
  public void testOnReportPlayer() {
    instance.onReport();

    verify(reportDialogController).setOffender(player);
    verify(reportDialogController).setOwnerWindow(instance.getRoot().getOwnerWindow());
    verify(reportDialogController).show();
  }

  @Test
  public void testOnJoinGame() {
    instance.onJoinGameSelected();

    verify(joinGameHelper).join(any());
  }

  @Test
  public void testOnWatchGame() {
    runOnFxThreadAndWait(() -> player.setGame(GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get()));
    instance.onWatchGameSelected();

    verify(replayService).runLiveReplay(player.getGame().getId());
  }

  @Test
  public void testOnWatchGameThrowsIoExceptionTriggersNotification() {
    doThrow(new RuntimeException("Error in runLiveReplay")).when(replayService).runLiveReplay(anyInt());
    player.setGame(GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get());

    instance.onWatchGameSelected();

    verify(notificationService).addImmediateErrorNotification(any(RuntimeException.class), eq("replays.live.loadFailure.message"));
  }

  @Test
  public void testKickContextMenuNotShownForNormalUser() {
    when(moderatorService.getPermissions()).thenReturn(Collections.emptySet());
    player.setSocialStatus(FOE);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.kickGameItem.isVisible());
    assertFalse(instance.kickLobbyItem.isVisible());
    assertFalse(instance.moderatorActionSeparator.isVisible());
  }

  @Test
  public void testKickContextMenuNotShownForSelf() {
    player.setSocialStatus(SELF);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.kickGameItem.isVisible());
    assertFalse(instance.kickLobbyItem.isVisible());
    assertFalse(instance.moderatorActionSeparator.isVisible());
  }

  @Test
  public void testKickContextMenuShownForModWithKickPermissions() {
    Set<String> permissions = Sets.newHashSet(GroupPermission.ADMIN_KICK_SERVER);
    when(moderatorService.getPermissions()).thenReturn(permissions);
    player.setSocialStatus(FOE);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.kickGameItem.isVisible());
    assertTrue(instance.kickLobbyItem.isVisible());
    assertTrue(instance.moderatorActionSeparator.isVisible());
  }
}
