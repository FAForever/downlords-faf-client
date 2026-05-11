package com.faforever.client.chat;

import com.faforever.client.preferences.ChatPrefs;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatMessageViewControllerVisibilityTest {

  @Test
  public void testMutedUserMessageIsNotVisible() {
    ChatPrefs chatPrefs = new ChatPrefs();
    ChatMessageViewController instance = new ChatMessageViewController(null, null, null, null, null, chatPrefs);
    ChatChannel chatChannel = new ChatChannel("#testChannel");
    ChatChannelUser user = new ChatChannelUser("junit", chatChannel);
    ChatMessage message = new ChatMessage("1", Instant.now(), user, "message", ChatMessage.Type.MESSAGE, null);

    assertTrue(instance.isVisibleMessage(message));

    chatPrefs.addMutedUser(user.getUsername().toUpperCase());

    assertFalse(instance.isVisibleMessage(message));
  }
}
