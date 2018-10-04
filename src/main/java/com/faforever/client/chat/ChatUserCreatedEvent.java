package com.faforever.client.chat;

import lombok.Value;

@Value
public class ChatUserCreatedEvent {
  ChatChannelUser chatChannelUser;
}
