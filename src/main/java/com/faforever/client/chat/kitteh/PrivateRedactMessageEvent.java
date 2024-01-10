package com.faforever.client.chat.kitteh;

import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.abstractbase.PrivateEventBase;
import org.kitteh.irc.client.library.event.helper.PrivateEvent;

import java.util.Optional;

/**
 * Fires when a redact is sent to the client.
 */
public class PrivateRedactMessageEvent extends PrivateEventBase<User> implements PrivateEvent, RedactMessageEvent {

  private final String redactedMessageId;
  private final String message;

  /**
   * Creates the event.
   *
   * @param client client for which this is occurring
   * @param sourceMessage source message
   * @param sender who sent it
   * @param target who received it
   * @param message message sent
   */
  public PrivateRedactMessageEvent(Client client, ServerMessage sourceMessage, User sender, String target,
                                   String message, String redactedMessageId) {
    super(client, sourceMessage, sender, target);
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
}
