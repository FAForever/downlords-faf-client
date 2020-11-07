package com.faforever.client.chat;

import com.faforever.client.game.PlayerStatus;
import com.faforever.client.player.Player;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.time.Instant;

public final class ChatChannelUserBuilder {
  private final ChatChannelUser chatChannelUser;

  private ChatChannelUserBuilder(String username) {
    chatChannelUser = new ChatChannelUser(username, Color.WHITE, false);
  }

  public static ChatChannelUserBuilder create(String username) {
    return new ChatChannelUserBuilder(username);
  }

  public ChatChannelUserBuilder defaultValues() {
    chatChannelUser.setDisplayed(true);
    return this;
  }

  public ChatChannelUser get() {
    return chatChannelUser;
  }

  public ChatChannelUserBuilder player(Player player) {
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

  public ChatChannelUserBuilder lastActive(Instant lastActive) {
    chatChannelUser.setLastActive(lastActive);
    return this;
  }

  public ChatChannelUserBuilder status(PlayerStatus status) {
    chatChannelUser.setStatus(status);
    return this;
  }

  public ChatChannelUserBuilder avatar(Image avatar) {
    chatChannelUser.setAvatar(avatar);
    return this;
  }

  public ChatChannelUserBuilder clanTag(String clanTag) {
    chatChannelUser.setClanTag(clanTag);
    return this;
  }

  public ChatChannelUserBuilder countryFlag(Image countryFlag) {
    chatChannelUser.setCountryFlag(countryFlag);
    return this;
  }

  public ChatChannelUserBuilder countryName(String countryName) {
    chatChannelUser.setCountryName(countryName);
    return this;
  }

  public ChatChannelUserBuilder mapImage(Image mapImage) {
    chatChannelUser.setMapImage(mapImage);
    return this;
  }

  public ChatChannelUserBuilder statusImage(Image statusImage) {
    chatChannelUser.setStatusImage(statusImage);
    return this;
  }

  public ChatChannelUserBuilder displayed(boolean displayed) {
    chatChannelUser.setDisplayed(displayed);
    return this;
  }

  public ChatChannelUserBuilder populated(boolean populated) {
    chatChannelUser.setPopulated(populated);
    return this;
  }
}
