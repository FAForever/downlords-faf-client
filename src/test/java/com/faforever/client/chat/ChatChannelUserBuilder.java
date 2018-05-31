package com.faforever.client.chat;

import com.faforever.client.player.Player;
import javafx.scene.paint.Color;

public final class ChatChannelUserBuilder {
  private final ChatChannelUser chatChannelUser;

  private ChatChannelUserBuilder(String username) {
    chatChannelUser = new ChatChannelUser(username, Color.WHITE, false);
  }

  public static ChatChannelUserBuilder create(String username) {
    return new ChatChannelUserBuilder(username);
  }

  public ChatChannelUserBuilder defaultValues() {
    return this;
  }

  public ChatChannelUser get() {
    return chatChannelUser;
  }

  public ChatChannelUserBuilder setPlayer(Player player) {
    chatChannelUser.setPlayer(player);
    return this;
  }

  public ChatChannelUserBuilder moderator(boolean isModerator) {
    chatChannelUser.setModerator(isModerator);
    return this;
  }
}
