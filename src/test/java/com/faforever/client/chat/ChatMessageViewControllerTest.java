package com.faforever.client.chat;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatMessageViewControllerTest {

  private ChatPrefs chatPrefs;
  private ChatMessageViewController instance;
  private ChatChannel chatChannel;

  @BeforeEach
  public void setUp() {
    chatPrefs = new ChatPrefs();
    instance = new ChatMessageViewController(null, null, null, null, null, chatPrefs);
    chatChannel = new ChatChannel("#test");
  }

  @Test
  public void testMutedUserMessagesHidden() {
    ChatMessage message = createMessage("muted");

    assertTrue(instance.shouldShowMessage(message));

    chatPrefs.muteUser("MUTED");

    assertFalse(instance.shouldShowMessage(message));
  }

  @Test
  public void testFoeMessagesHiddenWhenEnabled() {
    ChatMessage message = createMessage("foe");
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().socialStatus(SocialStatus.FOE).get();
    message.getSender().setPlayer(player);

    assertFalse(instance.shouldShowMessage(message));

    chatPrefs.setHideFoeMessages(false);

    assertTrue(instance.shouldShowMessage(message));
  }

  private ChatMessage createMessage(String username) {
    return new ChatMessage("1", Instant.now(), new ChatChannelUser(username, chatChannel), "message",
                           ChatMessage.Type.MESSAGE, null);
  }
}
