package com.faforever.client.theme;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Collection;

public interface ThemeService {

  String UNKNOWN_MAP_IMAGE = "images/unknown_map.png";
  String PLAYING_STATUS_IMAGE = "images/chat/playing.png";
  String HOSTING_STATUS_IMAGE = "images/chat/host.png";
  String LOBBY_STATUS_IMAGE = "images/chat/lobby.png";
  String DEFAULT_NEWS_IMAGE = "images/news_fallback.jpg";
  String STYLE_CSS = "style.css";
  String WEBVIEW_CSS_FILE = "style-webview.css";
  String DEFAULT_ACHIEVEMENT_IMAGE = "images/default_achievement.png";
  String MENTION_SOUND = "sounds/mention.mp3";
  String CSS_CLASS_FONTAWESOME = "fontawesome";
  String RANKED_1V1_IMAGE = "images/ranked1v1_notification.png";
  String TRAY_ICON = "images/tray_icon.png";

  String getThemeFile(String relativeFile);

  URL getThemeFileUrl(String relativeFile);

  void setTheme(Theme theme);

  /**
   * Unregisters a scene so it's no longer updated when the theme (or its CSS) changes.
   */
  void unregisterScene(Scene scene);

  /**
   * Registers a scene against the theme service so it can be updated whenever the theme (or its CSS) changes.
   */
  void registerScene(Scene scene);

  /**
   * Registers a WebView against the theme service so it can be updated whenever the theme changes.
   *
   * @param webView
   */
  void registerWebView(WebView webView);

  void loadThemes();

  Collection<Theme> getAvailableThemes();

  /**
   * Sets the application badge number to display on the application icon. An application badge is a small indicator
   * (e.g. a red circle) with a number on it, which indicates the number of (for instance) new messages or
   * notifications.
   *
   * @param stage
   * @param number the new badge number. If 0, the badge will be removed.
   */
  void setApplicationIconBadgeNumber(Stage stage, int number);
}
