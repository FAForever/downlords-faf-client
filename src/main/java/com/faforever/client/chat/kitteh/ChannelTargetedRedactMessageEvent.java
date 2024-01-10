package com.faforever.client.chat.kitteh;

import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.abstractbase.ActorChannelEventBase;
import org.kitteh.irc.client.library.event.helper.ChannelTargetedEvent;

import java.util.Optional;

/**
 * Fires when a redact is sent to a subset of users in a channel. Note that the sender may be the client itself if the
 * capability "echo-message" is enabled.
 */
public class ChannelTargetedRedactMessageEvent extends ActorChannelEventBase<User> implements ChannelTargetedEvent, RedactMessageEvent {

  private final String redactedMessageId;
  private final ChannelUserMode prefix;
  private final String message;

  /**
   * Creates the event.
   *
   * @param client client for which this is occurring
   * @param sourceMessage source message
   * @param sender who sent it
   * @param channel channel receiving
   * @param prefix targeted prefix
   * @param message message sent
   */
  public ChannelTargetedRedactMessageEvent(Client client, ServerMessage sourceMessage, User sender, Channel channel,
                                           ChannelUserMode prefix, String message, String redactedMessageId) {
    super(client, sourceMessage, sender, channel);
    this.prefix = prefix;
    this.message = message;
    this.redactedMessageId = redactedMessageId;
  }

  @Override
  public String getRedactedMessageId() {
    return redactedMessageId;
  }

  @Override
  public Optional<String> getRedactMessage() {
    return Optional.ofNullable(message);
  }

  @Override
  public ChannelUserMode getPrefix() {
    return prefix;
  }
}
