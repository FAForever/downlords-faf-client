package com.faforever.client.theme;

import com.faforever.client.fx.Controller;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.Collection;

public interface UiService {

  String UNKNOWN_MAP_IMAGE = "theme/images/unknown_map.png";
  //TODO: Create Images for News Categories
  String SERVER_UPDATE_NEWS_IMAGE = "theme/images/update.jpg";
  String LADDER_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  String TOURNAMENT_NEWS_IMAGE = "theme/images/ranked1v1_notification.png";
  String FA_UPDATE_NEWS_IMAGE = "theme/images/update.jpg";
  String LOBBY_UPDATE_NEWS_IMAGE = "theme/images/update.jpg";
  String BALANCE_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  String WEBSITE_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  String CAST_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  String PODCAST_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  String FEATURED_MOD_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  String DEVELOPMENT_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  String DEFAULT_NEWS_IMAGE = "theme/images/news_fallback.jpg";
  String STYLE_CSS = "theme/style.css";
  String WEBVIEW_CSS_FILE = "theme/style-webview.css";
  String DEFAULT_ACHIEVEMENT_IMAGE = "theme/images/default_achievement.png";
  String MENTION_SOUND = "theme/sounds/mention.mp3";
  String CSS_CLASS_ICON = "icon";
  String LADDER_1V1_IMAGE = "theme/images/ranked1v1_notification.png";
  String CHAT_CONTAINER = "theme/chat/chat_container.html";
  String CHAT_ENTRY = "theme/chat/chat_section.html";
  String CHAT_TEXT = "theme/chat/chat_text.html";

  Theme DEFAULT_THEME = new Theme() {
    {
      setAuthor("Downlord");
      setCompatibilityVersion(1);
      setDisplayName("Default");
      setThemeVersion("1.0");
    }
  };


  String getThemeFile(String relativeFile);

  /**
   * Loads an image from the current theme.
   */
  Image getThemeImage(String relativeImage);

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

  String[] getStylesheets();

  /**
   * Registers a WebView against the theme service so it can be updated whenever the theme changes.
   */
  void registerWebView(WebView webView);

  void loadThemes();

  Collection<Theme> getAvailableThemes();

  ReadOnlyObjectProperty<Theme> currentThemeProperty();

  /**
   * Loads an FXML file and returns its controller instance. The controller instance is retrieved from the application
   * context, so its scope (which should always be "prototype") depends on the bean definition.
   */
  <T extends Controller<?>> T loadFxml(String relativePath);
}
