package com.faforever.client.user;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.MenuItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PlayerContextMenuControllerTest extends UITest {

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

  private UserContextMenuController instance;
  private Player player;

  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBuilder.create("junit").get();

    instance = new UserContextMenuController(preferencesService, playerService,
        replayService, notificationService, i18n, eventBus, joinGameHelper,
        avatarService, uiService, moderatorService, teamMatchmakingService);
    loadFxml("theme/user/user_context_menu.fxml", clazz -> instance);
    instance.setPlayer(player);
  }

  @Test
  public void testSetAlreadyPlayerException() {
    assertThrows(IllegalStateException.class, () -> instance.setPlayer(PlayerBuilder.create("junit").get()));
  }

  @Test
  public void testNoChatUserItems() {
    assertFalse(instance.chatChannelSeparator.isVisible());
    assertFalse(instance.colorPickerMenuItem.isVisible());
    assertFalse(instance.avatarPickerMenuItem.isVisible());
  }

  @Test
  public void testNoModerationItems() {
    assertFalse(instance.moderatorActionSeparator.isVisible());
    assertFalse(instance.kickGameItem.isVisible());
    assertFalse(instance.kickLobbyItem.isVisible());
    assertFalse(instance.broadcastMessage.isVisible());
  }

  @Test
  public void testNoSocialItems() {
    assertFalse(instance.socialSeparator.isVisible());
    assertFalse(instance.invitePlayerItem.isVisible());
    assertFalse(instance.addFriendItem.isVisible());
    assertFalse(instance.removeFriendItem.isVisible());
    assertFalse(instance.addFoeItem.isVisible());
    assertFalse(instance.removeFoeItem.isVisible());
    assertFalse(instance.reportPlayerItem.isVisible());
  }
}
