package com.faforever.client.builders;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.domain.PlayerBean;
import javafx.scene.paint.Color;

public final class ChatChannelUserBuilder {
  private final ChatChannelUser chatChannelUser;

  private ChatChannelUserBuilder(String username, String channel) {
    chatChannelUser = new ChatChannelUser(username, channel);
  }

  public static ChatChannelUserBuilder create(String username, String channel) {
    return new ChatChannelUserBuilder(username, channel);
  }

  public ChatChannelUserBuilder defaultValues() {
    return this;
  }

  public ChatChannelUser get() {
    return chatChannelUser;
  }

  public ChatChannelUserBuilder player(PlayerBean player) {
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
