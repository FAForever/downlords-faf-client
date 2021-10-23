package com.faforever.client.fx;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static com.faforever.client.player.SocialStatus.SELF;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractPlayerContextMenuControllerTest extends UITest {
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

  private AbstractPlayerContextMenuController instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new AbstractPlayerContextMenuController(avatarService, eventBus, i18n, joinGameHelper, moderatorService,
        notificationService, playerService, replayService, uiService) {};
    loadFxml("theme/player_context_menu.fxml", clazz -> instance, instance);

    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(TEST_USER_NAME).socialStatus(SELF).avatar(null).game(new GameBean()).get();
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
    assertThat(instance.avatarPickerMenuItem.isVisible(), is(false));
  }
}
