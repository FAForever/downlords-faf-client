package com.faforever.client.news;

public final class UnreadNewsEvent {
  private final boolean hasUnreadNews;

  public UnreadNewsEvent(boolean hasUnreadNews) {
    this.hasUnreadNews = hasUnreadNews;
  }

  public boolean hasUnreadNews() {
    return hasUnreadNews;
  }
}
