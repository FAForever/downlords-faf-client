package com.faforever.client.player;

import com.faforever.client.chat.ChatChannelUser;
import lombok.Value;

@Value
public class PlayerSocialChangeEvent {
  ChatChannelUser chatUser;
}
