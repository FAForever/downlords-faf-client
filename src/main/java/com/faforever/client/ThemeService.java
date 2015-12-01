package com.faforever.client;

public interface ThemeService {

  String UNKNOWN_MAP_IMAGE = "images/unknown_map.png";
  String PLAYING_STATUS_IMAGE = "images/chat/playing.png";
  String HOSTING_STATUS_IMAGE = "images/chat/host.png";
  String LOBBY_STATUS_IMAGE = "images/chat/lobby.png";
  String DEFAULT_NEWS_IMAGE = "images/news_fallback.jpg";
  String WEBVIEW_CSS_FILE = "style-webview.css";
  String DEFAULT_ACHIEVEMENT_IMAGE = "images/default_achievement.png";
  String MENTION_SOUND = "sounds/mention.mp3";

  String getThemeFile(String relativeFile);
}
