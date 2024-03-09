package com.faforever.client.chat.kitteh.event;

import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.abstractbase.ActorChannelEventBase;
import org.kitteh.irc.client.library.event.helper.ActorEvent;

import java.util.Optional;

/**
 * Fires when a redact is sent to a channel. Note that the sender may be the client itself if the capability
 * "echo-message" is enabled.
 */
public class ChannelRedactMessageEvent extends ActorChannelEventBase<User> implements ActorEvent<User>, RedactMessageEvent {

  private final String redactedMessageId;
  private final String message;

  /**
   * Creates the event.
   *
   * @param client client for which this is occurring
   * @param sourceMessage source message
   * @param sender who sent it
   * @param channel channel receiving
   * @param message message sent
   */
  public ChannelRedactMessageEvent(Client client, ServerMessage sourceMessage, User sender, Channel channel,
                                   String message, String redactedMessageId) {
    super(client, sourceMessage, sender, channel);
    this.redactedMessageId = redactedMessageId;
    this.message = message;
  }

  @Override
  public String getRedactedMessageId() {
    return redactedMessageId;
  }

  @Override
  public Optional<String> getRedactMessage() {
    return Optional.ofNullable(message);
  }
}
