package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.player.SocialStatus.SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PartyMemberContextMenuControllerTest extends AbstractPlainJavaFxTest {
  private static final String TEST_USER_NAME = "junit";
  
  @Mock
  private ClientProperties clientProperties;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;
  @Mock
  private PlatformService platformService;
  
  private PartyMemberContextMenuController instance;
  private Player player;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new PartyMemberContextMenuController(clientProperties, playerService,
        eventBus, uiService, platformService);
    loadFxml("theme/play/teammatchmaking/party_member_context_menu.fxml", clazz -> instance);

    player = PlayerBuilder.create(TEST_USER_NAME).socialStatus(SELF).avatar(null).game(new Game()).get();
  }

  @Test
  public void testOnSendPrivateMessage() {
    instance.setPlayer(player);

    instance.onSendPrivateMessageSelected();

    verify(eventBus).post(any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testOnAddFriendWithFoe() {
    instance.setPlayer(player);

    player.setSocialStatus(FOE);

    instance.onAddFriendSelected();

    verify(playerService).removeFoe(player);
    verify(playerService).addFriend(player);
  }

  @Test
  public void testOnAddFriendWithNeutral() {
    player.setSocialStatus(OTHER);

    instance.setPlayer(player);

    instance.onAddFriendSelected();

    verify(playerService, never()).removeFoe(player);
    verify(playerService).addFriend(player);
  }

  @Test
  public void testOnRemoveFriend() {
    instance.setPlayer(player);

    instance.onRemoveFriendSelected();

    verify(playerService).removeFriend(player);
  }

  @Test
  public void testOnAddFoeWithFriend() {
    instance.setPlayer(player);

    player.setSocialStatus(FRIEND);

    instance.onAddFoeSelected();

    verify(playerService).removeFriend(player);
    verify(playerService).addFoe(player);
  }

  @Test
  public void testOnAddFoeWithNeutral() {
    instance.setPlayer(player);

    player.setSocialStatus(OTHER);

    instance.onAddFoeSelected();

    verify(playerService, never()).removeFriend(player);
    verify(playerService).addFoe(player);
  }

  @Test
  public void testOnRemoveFoe() {
    instance.setPlayer(player);

    instance.onRemoveFoeSelected();

    verify(playerService).removeFoe(player);
  }

}
