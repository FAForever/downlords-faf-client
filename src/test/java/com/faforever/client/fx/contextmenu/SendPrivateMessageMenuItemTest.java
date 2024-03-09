package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.chat.ChatService;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendPrivateMessageMenuItemTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private PlayerService playerService;
  @Mock
  private ChatService chatService;

  private SendPrivateMessageMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new SendPrivateMessageMenuItem(i18n, playerService, chatService);
  }

  @Test
  public void testSendPrivateMessage() {
    PlayerInfo ownPlayer = PlayerInfoBuilder.create().defaultValues().username("junit").get();
    when(playerService.getCurrentPlayer()).thenReturn(ownPlayer);
    String username = "junit";

    instance.setObject(username);
    instance.onClicked();

    verify(chatService).joinPrivateChat(any());
  }

  @Test
  public void testVisibleItemIfPlayerIsNotOwnPlayer() {
    PlayerInfo ownPlayer = PlayerInfoBuilder.create().defaultValues().username("own player").get();
    when(playerService.getCurrentPlayer()).thenReturn(ownPlayer);
    when(chatService.userExistsInAnyChannel(anyString())).thenReturn(true);
    String username = "junit";

    instance.setObject(username);

    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsOwnPlayer() {
    PlayerInfo ownPlayer = PlayerInfoBuilder.create().defaultValues().username("own player").get();
    when(playerService.getCurrentPlayer()).thenReturn(ownPlayer);
    String username = "own player";

    instance.setObject(username);

    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUsernameIsBlank() {
    PlayerInfo ownPlayer = PlayerInfoBuilder.create().defaultValues().username("junit").get();
    instance.setObject("");
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUsernameIsNull() {
    PlayerInfo ownPlayer = PlayerInfoBuilder.create().defaultValues().username("junit").get();
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerNotInChat() {
    PlayerInfo ownPlayer = PlayerInfoBuilder.create().defaultValues().username("junit").get();
    instance.setObject("");
    assertFalse(instance.isVisible());
  }
}