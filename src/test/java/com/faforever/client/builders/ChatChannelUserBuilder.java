package com.faforever.client.builders;

import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.domain.server.PlayerInfo;
import javafx.scene.paint.Color;

public final class ChatChannelUserBuilder {
  private final ChatChannelUser chatChannelUser;

  private ChatChannelUserBuilder(String username, ChatChannel channel) {
    chatChannelUser = new ChatChannelUser(username, channel);
  }

  public static ChatChannelUserBuilder create(String username, ChatChannel channel) {
    return new ChatChannelUserBuilder(username, channel);
  }

  public ChatChannelUserBuilder defaultValues() {
    return this;
  }

  public ChatChannelUser get() {
    return chatChannelUser;
  }

  public ChatChannelUserBuilder player(PlayerInfo player) {
    chatChannelUser.setPlayer(player);
    return this;
  }

  public ChatChannelUserBuilder moderator(boolean isModerator) {
    chatChannelUser.setModerator(isModerator);
    return this;
  }

  public ChatChannelUserBuilder color(Color color) {
    chatChannelUser.setColor(color);
    return this;
  }
}
