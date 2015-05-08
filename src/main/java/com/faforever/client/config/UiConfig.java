package com.faforever.client.config;

import com.faforever.client.chat.AvatarService;
import com.faforever.client.chat.AvatarServiceImpl;
import com.faforever.client.chat.ChannelTabFactory;
import com.faforever.client.chat.ChannelTabFactoryImpl;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatUserControlFactory;
import com.faforever.client.chat.ChatUserControlFactoryImpl;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.CountryFlagServiceImpl;
import com.faforever.client.chat.UserInfoWindowFactory;
import com.faforever.client.chat.UserInfoWindowFactoryImpl;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.SceneFactoryImpl;
import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.fxml.FxmlLoaderImpl;
import com.faforever.client.game.CreateGameDialogFactory;
import com.faforever.client.game.CreateGameDialogFactoryImpl;
import com.faforever.client.game.GamesController;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.MainController;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl;
import com.faforever.client.whatsnew.WhatsNewController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
  WhatsNewController whatsNewController() {
    return mainController().getWhatsNewController();
  }

  @Bean
  GamesController gamesController() {
    return mainController().getGamesController();
  }

  @Bean
  ChatController chatController() {
    return mainController().getChatController();
  }

  @Bean
  ChannelTabFactory chatTabFactory() {
    return new ChannelTabFactoryImpl();
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
    return new MapServiceImpl();
  }

  @Bean
  CreateGameDialogFactory createGameDialogFactory() { return new CreateGameDialogFactoryImpl(); }

  @Bean
  UserInfoWindowFactory userInfoWindowFactory() {
    return new UserInfoWindowFactoryImpl();
  }

  @Bean
  FxmlLoader fxmlLoader() {
    FxmlLoaderImpl fxmlLoader = new FxmlLoaderImpl(baseConfig.messageSource(), baseConfig.locale());
    fxmlLoader.setTheme(baseConfig.preferencesService().getPreferences().getTheme());
    return fxmlLoader;
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
