package com.faforever.client.config;

import com.faforever.client.audio.AudioClipPlayer;
import com.faforever.client.audio.AudioClipPlayerImpl;
import com.faforever.client.audio.AudioController;
import com.faforever.client.audio.AudioControllerImpl;
import com.faforever.client.cast.CastsController;
import com.faforever.client.chat.AvatarService;
import com.faforever.client.chat.AvatarServiceImpl;
import com.faforever.client.chat.ChannelTabController;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatUserContextMenuController;
import com.faforever.client.chat.ChatUserControl;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.CountryFlagServiceImpl;
import com.faforever.client.chat.PlayerInfoTooltipController;
import com.faforever.client.chat.PrivateChatTabController;
import com.faforever.client.chat.UrlPreviewResolver;
import com.faforever.client.chat.UrlPreviewResolverImpl;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.DialogFactory;
import com.faforever.client.fx.DialogFactoryImpl;
import com.faforever.client.fx.FxmlLoader;
import com.faforever.client.fx.FxmlLoaderImpl;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.SceneFactoryImpl;
import com.faforever.client.game.CreateGameController;
import com.faforever.client.game.EnterPasswordController;
import com.faforever.client.game.GameCardController;
import com.faforever.client.game.GameContainerTooltipController;
import com.faforever.client.game.GameTableController;
import com.faforever.client.game.GameTiledController;
import com.faforever.client.game.GamesController;
import com.faforever.client.game.PlayerCardTooltipController;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.hub.CommunityHubController;
import com.faforever.client.hub.ConcurrentUsersController;
import com.faforever.client.hub.DonationWallController;
import com.faforever.client.hub.LastCastController;
import com.faforever.client.hub.LastNewsController;
import com.faforever.client.hub.MapOfTheDayController;
import com.faforever.client.hub.MostActivePlayersController;
import com.faforever.client.hub.RecentForumPostsController;
import com.faforever.client.hub.TopPlayersController;
import com.faforever.client.hub.UpcomingEventsController;
import com.faforever.client.leaderboard.LeaderboardController;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.MainController;
import com.faforever.client.map.CommentCardController;
import com.faforever.client.map.MapPreviewLargeController;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.news.NewsController;
import com.faforever.client.news.NewsListItemController;
import com.faforever.client.notification.ImmediateNotificationController;
import com.faforever.client.notification.PersistentNotificationController;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.preferences.SettingsController;
import com.faforever.client.replay.ReplayVaultController;
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
  DialogFactory dialogFactory() {
    return new DialogFactoryImpl();
  }

  @Bean
  LoginController loginController() {
    return loadController("login.fxml");
  }

  private <T> T loadController(String fxml) {
    return fxmlLoader().loadAndGetController(fxml);
  }

  @Bean
  FxmlLoader fxmlLoader() {
    return new FxmlLoaderImpl();
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
  MapPreviewLargeController mapPreviewLargeController() {
    return loadController("map_preview_large.fxml");
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
  CommunityHubController communityHubController() {
    return loadController("community_hub.fxml");
  }

  @Bean
  ConcurrentUsersController concurrentUsersController() {
    return loadController("concurrent_users.fxml");
  }

  @Bean
  LastCastController lastCastController() {
    return loadController("last_cast.fxml");
  }

  @Bean
  UpcomingEventsController upcomingEventController() {
    return loadController("upcoming_events.fxml");
  }

  @Bean
  LastNewsController lastNewsController() {
    return loadController("last_news.fxml");
  }

  @Bean
  MapOfTheDayController mapOfTheDayController() {
    return loadController("map_of_the_day.fxml");
  }

  @Bean
  TopPlayersController topPlayersController() {
    return loadController("top_players.fxml");
  }

  @Bean
  DonationWallController donationWallController() {
    return loadController("donation_wall.fxml");
  }

  @Bean
  RecentForumPostsController recentForumPostsController() {
    return loadController("recent_forum_posts.fxml");
  }

  @Bean
  MostActivePlayersController mostActivePlayersController() {
    return loadController("most_active_players.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  CommentCardController commentCardController() {
    return loadController("comment_card.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    // TODO cs: naming consistency
  GameTableController gameTableController() {
    return loadController("game_table.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ImmediateNotificationController immediateNotificationController() {
    return loadController("immediate_notification.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  PlayerCardTooltipController playerCardTooltipController() {
    return loadController("player_card_tooltip.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  TeamCardController teamCardController() {
    return loadController("team_card.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  GameContainerTooltipController gameContainerTooltipController() {
    return loadController("game_container_tooltip.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    // TODO @cs: naming consistency
  GameTiledController gamesTiledController() {
    return loadController("game_tile_container.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  UserInfoWindowController userInfoWindowController() {
    return loadController("user_info_window.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    // TODO @cs: naming consistency
  GameCardController gameCardController() {
    return loadController("game_tile.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    // TODO @mj: naming consistency
  NewsListItemController newsTileController() {
    return loadController("news_list_item.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  PersistentNotificationController persistentNotificationController() {
    return loadController("persistent_notification.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ChatUserControl chatUserControl() {
    return new ChatUserControl();
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
  ReplayVaultController replayVaultController() {
    return loadController("replay_vault.fxml");
  }

  @Bean
  ModVaultController modVaultController() {
    return loadController("mod_vault.fxml");
  }

  @Bean
  MapVaultController mapVaultController() {
    return loadController("map_vault.fxml");
  }

  @Bean
  CastsController castsController() {
    return loadController("casts.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ChannelTabController channelTab() {
    return loadController("channel_tab.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  PrivateChatTabController privateChatTab() {
    return loadController("private_chat_tab.fxml");
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
  SettingsController settingsWindowController() {
    return loadController("settings.fxml");
  }

  @Bean
  AudioClipPlayer audioClipPlayer() {
    return new AudioClipPlayerImpl();
  }

  @Bean
  AudioController soundService() {
    return new AudioControllerImpl();
  }

  @Bean
  UrlPreviewResolver urlPreviewResolver() {
    return new UrlPreviewResolverImpl();
  }
}
