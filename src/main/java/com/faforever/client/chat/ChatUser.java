package com.faforever.client.chat;

import org.pircbotx.User;

import java.util.Comparator;

public class ChatUser {

  public static final Comparator<ChatUser> SORT_BY_NAME_COMPARATOR = new Comparator<ChatUser>() {
    @Override
    public int compare(ChatUser o1, ChatUser o2) {
      return o1.getLogin().compareTo(o2.getLogin());
    }
  };

  private User user;

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

  public String getLogin() {
    return user.getLogin();
  }
}
