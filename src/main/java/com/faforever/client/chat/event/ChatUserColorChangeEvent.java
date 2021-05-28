package com.faforever.client.chat.event;

import com.faforever.client.chat.ChatChannelUser;
import lombok.Value;

@Value
public class ChatUserColorChangeEvent {
  ChatChannelUser chatUser;
}
