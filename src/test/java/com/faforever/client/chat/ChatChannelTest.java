package com.faforever.client.chat;


import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.chat.ChatMessage.Type;
import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.Reaction;
import com.faforever.client.test.DomainTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
    ChatMessage message1 = new ChatMessage("1", Instant.now().minusSeconds(1), sender, "1", Type.MESSAGE, null);
    channel.addMessage(message1);
    ChatMessage message2 = new ChatMessage("2", Instant.now(), sender, "2", Type.MESSAGE, null);
    channel.addMessage(message2);

    assertThat(channel.getMessages(), hasSize(2));

    channel.setMaxNumMessages(1);

    assertThat(channel.getMessages(), hasSize(1));
    assertThat(channel.getMessages(), contains(message2));

    ChatMessage message3 = new ChatMessage("3", Instant.now(), sender, "3", Type.MESSAGE, null);
    channel.addMessage(message3);

    assertThat(channel.getMessages(), hasSize(1));
    assertThat(channel.getMessages(), contains(message3));
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

  @Test
  public void testReactions() {
    ChatChannel channel = new ChatChannel("#test");
    ChatChannelUser sender = ChatChannelUserBuilder.create("", channel).defaultValues().get();
    ChatMessage message = new ChatMessage("1", Instant.now().minusSeconds(1), sender, "1", Type.MESSAGE, null);
    channel.addMessage(message);

    Emoticon emoticon = new Emoticon(List.of(), "");
    channel.addReaction(new Reaction("2", "1", emoticon, "junit"));

    assertThat(message.getReactions(), hasKey(emoticon));
    assertThat(message.getReactions().get(emoticon), hasKey("junit"));
    assertThat(message.getReactions().get(emoticon).get("junit"), equalTo("2"));

    channel.removeMessage("2");
    assertThat(message.getReactions(), not(hasKey(emoticon)));
  }

}
