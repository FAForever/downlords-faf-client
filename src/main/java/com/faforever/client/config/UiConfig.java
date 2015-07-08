package com.faforever.client.config;

import com.faforever.client.chat.AvatarService;
import com.faforever.client.chat.AvatarServiceImpl;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatTabFactory;
import com.faforever.client.chat.ChatTabFactoryImpl;
import com.faforever.client.chat.ChatUserContextMenuController;
import com.faforever.client.chat.ChatUserControlFactory;
import com.faforever.client.chat.ChatUserControlFactoryImpl;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.CountryFlagServiceImpl;
import com.faforever.client.chat.PlayerInfoTooltipController;
import com.faforever.client.chat.UrlPreviewResolver;
import com.faforever.client.chat.UrlPreviewResolverImpl;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.SceneFactoryImpl;
import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.fxml.FxmlLoaderImpl;
import com.faforever.client.game.CreateGameController;
import com.faforever.client.game.EnterPasswordController;
import com.faforever.client.game.GamesController;
import com.faforever.client.leaderboard.LeaderboardController;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.MainController;
import com.faforever.client.news.NewsController;
import com.faforever.client.news.NewsListItemController;
import com.faforever.client.notification.NotificationNodeFactory;
import com.faforever.client.notification.NotificationNodeFactoryImpl;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.preferences.SettingsController;
import com.faforever.client.replay.ReplayVaultController;
import com.faforever.client.sound.AudioClipPlayer;
import com.faforever.client.sound.AudioClipPlayerImpl;
import com.faforever.client.sound.SoundController;
import com.faforever.client.sound.SoundControllerImpl;
import com.faforever.client.vault.VaultController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

@org.springframework.context.annotation.Configuration
@Import(BaseConfig.class)
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
  LeaderboardController leaderboardController() {
    return loadController("ladder.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  UserInfoWindowController userInfoWindowController() {
    return loadController("user_info_window.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  NewsListItemController newsTileController() {
    return loadController("news_list_item.fxml");
  }

  @Bean
  PlayerInfoTooltipController playerInfoTooltipController() {
    return loadController("player_info_tooltip.fxml");
  }

  @Bean
  ChatUserContextMenuController chatUserContextMenuController() {
    return loadController("chat_user_context_menu.fxml");
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
  ReplayVaultController replayVaultController() {
    return loadController("replay_vault.fxml");
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
  NotificationNodeFactory notificationPaneFactory() {
    return new NotificationNodeFactoryImpl();
  }

  @Bean
  SettingsController settingsWindowController() {
    return loadController("settings.fxml");
  }

  @Bean
  AudioClipPlayer audioClipPlayer() {
    return new AudioClipPlayerImpl();
  }

  @Bean
  SoundController soundService() {
    return new SoundControllerImpl();
  }

  @Bean
  UrlPreviewResolver urlPreviewResolver() {
    return new UrlPreviewResolverImpl();
  }

  @Bean
  FxmlLoader fxmlLoader() {
    return new FxmlLoaderImpl();
  }

  private <T> T loadController(String fxml) {
    return fxmlLoader().loadAndGetController(fxml);
  }
}
