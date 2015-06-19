package com.faforever.client.config;

import com.faforever.client.chat.AvatarService;
import com.faforever.client.chat.AvatarServiceImpl;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatTabFactory;
import com.faforever.client.chat.ChatTabFactoryImpl;
import com.faforever.client.chat.ChatUserControlFactory;
import com.faforever.client.chat.ChatUserControlFactoryImpl;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.CountryFlagServiceImpl;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.SceneFactoryImpl;
import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.fxml.FxmlLoaderImpl;
import com.faforever.client.game.CreateGameController;
import com.faforever.client.game.EnterPasswordController;
import com.faforever.client.game.GamesController;
import com.faforever.client.leaderboard.LadderController;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.MainController;
import com.faforever.client.map.LegacyMapService;
import com.faforever.client.map.MapService;
import com.faforever.client.news.NewsController;
import com.faforever.client.notification.NotificationNodeFactory;
import com.faforever.client.notification.NotificationNodeFactoryImpl;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.vault.VaultController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@org.springframework.context.annotation.Configuration
@Import(BaseConfig.class)
@EnableCaching
public class UiConfig {

  @Autowired
  Environment environment;

  @Autowired
  BaseConfig baseConfig;

  @Bean
  SceneFactory sceneFactory() {
    return new SceneFactoryImpl();
  }

  @Bean
  LoginController loginController() {
    return loadController("login.fxml");
  }

  @Bean
  MainController mainController() {
    return loadController("main.fxml");
  }

  @Bean
  NewsController newsController() {
    return loadController("news.fxml");
  }

  @Bean
  GamesController gamesController() {
    return loadController("games.fxml");
  }

  @Bean
  ChatController chatController() {
    return loadController("chat.fxml");
  }

  @Bean
  LadderController leaderboardController() {
    return loadController("ladder.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  UserInfoWindowController userInfoWindowController() {
    return loadController("user_info_window.fxml");
  }


  @Bean
  PersistentNotificationsController notificationsController() {
    return loadController("persistent_notifications.fxml");
  }

  @Bean
  CreateGameController createGameController() {
    return loadController("create_game.fxml");
  }

  @Bean
  EnterPasswordController enterPasswordController() {
    return loadController("enter_password.fxml");
  }

  @Bean
  VaultController vaultController() {
    return loadController("vault.fxml");
  }

  @Bean
  ChatTabFactory chatTabFactory() {
    return new ChatTabFactoryImpl();
  }

  @Bean
  ChatUserControlFactory userEntryFactory() {
    return new ChatUserControlFactoryImpl();
  }

  @Bean
  AvatarService avatarService() {
    return new AvatarServiceImpl();
  }

  @Bean
  CountryFlagService countryFlagService() {
    return new CountryFlagServiceImpl();
  }

  @Bean
  MapService mapPreviewService() {
    return new LegacyMapService();
  }

  @Bean
  NotificationNodeFactory notificationPaneFactory() {
    return new NotificationNodeFactoryImpl();
  }

  @Bean
  FxmlLoader fxmlLoader() {
    String theme = baseConfig.preferencesService().getPreferences().getTheme();
    return new FxmlLoaderImpl(baseConfig.messageSource(), baseConfig.locale(), theme);
  }

  @Bean
  public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    cacheManager.setCaches(Arrays.asList(
        new ConcurrentMapCache("avatars"),
        new ConcurrentMapCache("countryFlags"),
        new ConcurrentMapCache("smallMapPreview"),
        new ConcurrentMapCache("largeMapPreview")
    ));
    return cacheManager;
  }

  private <T> T loadController(String fxml) {
    return fxmlLoader().loadAndGetController(fxml);
  }
}
