package com.faforever.client.chat.kitteh.event;

import org.kitteh.irc.client.library.event.helper.ClientEvent;

import java.util.Optional;

public interface RedactMessageEvent extends ClientEvent {

  String getRedactedMessageId();

  Optional<String> getRedactMessage();

}
