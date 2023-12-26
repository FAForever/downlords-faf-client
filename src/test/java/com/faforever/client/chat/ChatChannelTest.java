package com.faforever.client.chat;


import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.test.DomainTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatChannelTest extends DomainTest {

  @Test
  public void testPrivateChannel() {
    assertTrue(new ChatChannel("me").isPrivateChannel());
    assertFalse(new ChatChannel("#me").isPrivateChannel());
  }

  @Test
  public void testPartyChannel() {
    assertTrue(new ChatChannel("#me" + ChatService.PARTY_CHANNEL_SUFFIX).isPartyChannel());
    assertFalse(new ChatChannel("#me").isPartyChannel());
    assertFalse(new ChatChannel("me" + ChatService.PARTY_CHANNEL_SUFFIX).isPartyChannel());
  }

  @Test
  public void testMessageMax() {
    ChatChannel channel = new ChatChannel("#test");
    ChatChannelUser sender = ChatChannelUserBuilder.create("", channel).defaultValues().get();
    channel.addMessage(new ChatMessage(Instant.now(), sender, "1"));
    channel.addMessage(new ChatMessage(Instant.now(), sender, "2"));

    assertThat(channel.getMessages(), hasSize(2));

    channel.setMaxNumMessages(1);

    assertThat(channel.getMessages(), hasSize(1));
    assertEquals("2", channel.getMessages().getLast().message());

    channel.addMessage(new ChatMessage(Instant.now(), sender, "3"));

    assertThat(channel.getMessages(), hasSize(1));
    assertEquals("3", channel.getMessages().getLast().message());
  }

  @Test
  public void testTypingUsers() {
    ChatChannel channel = new ChatChannel("#test");
    ChatChannelUser me = channel.createUserIfNecessary("me", user -> {});
    assertThat(channel.getUsers(), hasSize(1));
    assertThat(channel.getTypingUsers(), empty());

    me.setTyping(true);
    assertThat(channel.getUsers(), hasSize(1));
    assertThat(channel.getTypingUsers(), hasSize(1));
  }

  @Test
  public void testOpeningChannelSetsUnreadToZero() {
    ChatChannel channel = new ChatChannel("#test");
    channel.setNumUnreadMessages(10);

    channel.setOpen(true);

    assertEquals(0, channel.getNumUnreadMessages());
  }

}
