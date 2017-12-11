package com.faforever.client.news;


import com.faforever.client.theme.UiService;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum NewsCategory {

  SERVER_UPDATE("server update", UiService.SERVER_UPDATE_NEWS_IMAGE),
  TOURNAMENT("tournament", UiService.TOURNAMENT_NEWS_IMAGE),
  FA_UPDATE("fa update", UiService.FA_UPDATE_NEWS_IMAGE),
  LOBBY_UPDATE("lobby update", UiService.LOBBY_UPDATE_NEWS_IMAGE),
  BALANCE("balance", UiService.BALANCE_NEWS_IMAGE),
  WEBSITE("website", UiService.WEBSITE_NEWS_IMAGE),
  CAST("cast", UiService.CAST_NEWS_IMAGE),
  PODCAST("podcast", UiService.PODCAST_NEWS_IMAGE),
  FEATURED_MOD("featured mods", UiService.FEATURED_MOD_NEWS_IMAGE),
  DEVELOPMENT("development update", UiService.DEVELOPMENT_NEWS_IMAGE),
  UNCATEGORIZED("uncategorized", UiService.DEFAULT_NEWS_IMAGE),
  LADDER("ladder", UiService.LADDER_NEWS_IMAGE);

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
    if (string == null) {
      return null;
    }
    String toLower = string.toLowerCase(Locale.US);
    if (!fromString.containsKey(toLower)) {
      return NewsCategory.UNCATEGORIZED;
    }
    return fromString.get(toLower);
  }

  public String getImagePath() {
    return imagePath;
  }

}
