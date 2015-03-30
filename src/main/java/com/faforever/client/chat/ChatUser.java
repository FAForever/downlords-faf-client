package com.faforever.client.chat;

import org.pircbotx.User;

public class ChatUser {

  private User user;
  private String avatar;
  private String country;

  public ChatUser(User user) {
    this.user = user;
  }

  @Override
  public boolean equals(Object obj) {
    return obj.getClass() == ChatUser.class && user.equals(obj);
  }

  @Override
  public int hashCode() {
    return user.hashCode();
  }

  public String getNick() {
    return user.getNick();
  }

  public String getAvatar() {
    return avatar;
  }

  public String getCountry() {
    return country;
  }
}
