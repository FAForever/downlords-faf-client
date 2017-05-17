package com.faforever.client.news;


import com.faforever.client.theme.UiService;

import java.util.HashMap;
import java.util.Map;

public enum NewsCategory {

  SERVER_UPDATE("Server Update", UiService.SERVER_UPDATE_NEWS_IMAGE),
  TOURNAMENT("Tournament", UiService.TOURNAMENT_NEWS_IMAGE),
  FA_UPDATE("FA Update", UiService.FA_UPDATE_NEWS_IMAGE),
  LOBBY_UPDATE("Lobby Update", UiService.LOBBY_UPDATE_NEWS_IMAGE),
  BALANCE("Balance", UiService.BALANCE_NEWS_IMAGE),
  WEBSITE("website", UiService.WEBSITE_NEWS_IMAGE),
  CAST("Cast", UiService.CAST_NEWS_IMAGE),
  PODCAST("Podcast", UiService.PODCAST_NEWS_IMAGE),
  FEATURED_MOD("featured mods", UiService.FEATURED_MOD_NEWS_IMAGE),
  DEVELOPMENT("Development Update", UiService.DEVELOPMENT_NEWS_IMAGE),
  UNCATEGORIZED("Uncategorized", UiService.DEFAULT_NEWS_IMAGE);

  private static final Map<String, NewsCategory> fromString;

  static {
    fromString = new HashMap<>();
    for (NewsCategory newsCategory : values()) {
      fromString.put(newsCategory.name, newsCategory);
    }
  }

  private final String name;
  private final String imagePath;


  NewsCategory(String name, String imagePath) {
    this.name = name;
    this.imagePath = imagePath;
  }

  public static NewsCategory fromString(String string) {
    if (!fromString.containsKey(string)) {
      return NewsCategory.UNCATEGORIZED;
    }
    return fromString.get(string);
  }

  public String getImagePath() {
    return imagePath;
  }

}
