package com.faforever.client.config;

import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementUnlockedNotifier;
import com.faforever.client.audio.AudioClipPlayer;
import com.faforever.client.audio.AudioClipPlayerImpl;
import com.faforever.client.audio.AudioController;
import com.faforever.client.audio.AudioControllerImpl;
import com.faforever.client.cast.CastsController;
import com.faforever.client.chat.AutoCompletionHelper;
import com.faforever.client.chat.ChannelTabController;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatUserContextMenuController;
import com.faforever.client.chat.ChatUserItemController;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.CountryFlagServiceImpl;
import com.faforever.client.chat.FilterUserController;
import com.faforever.client.chat.GameStatusTooltipController;
import com.faforever.client.chat.PrivateChatTabController;
import com.faforever.client.chat.UrlPreviewResolver;
import com.faforever.client.chat.UrlPreviewResolverImpl;
import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.chat.avatar.AvatarServiceImpl;
import com.faforever.client.fa.OnGameFullNotifier;
import com.faforever.client.fx.DialogFactory;
import com.faforever.client.fx.DialogFactoryImpl;
import com.faforever.client.fx.FxmlLoader;
import com.faforever.client.fx.FxmlLoaderImpl;
import com.faforever.client.fx.WindowController;
import com.faforever.client.game.CreateGameController;
import com.faforever.client.game.EnterPasswordController;
import com.faforever.client.game.GameTileController;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.game.GamesController;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.game.GamesTilesContainerController;
import com.faforever.client.game.JoinGameHelper;
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
import com.faforever.client.main.UserMenuController;
import com.faforever.client.map.MapDetailController;
import com.faforever.client.map.MapTileController;
import com.faforever.client.map.MapUploadController;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.mod.ModDetailController;
import com.faforever.client.mod.ModTileController;
import com.faforever.client.mod.ModUploadController;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.news.NewsController;
import com.faforever.client.news.NewsListItemController;
import com.faforever.client.notification.ImmediateNotificationController;
import com.faforever.client.notification.PersistentNotificationController;
import com.faforever.client.notification.PersistentNotificationsController;
import com.faforever.client.notification.TransientNotificationController;
import com.faforever.client.notification.TransientNotificationsController;
import com.faforever.client.player.FriendJoinedGameNotifier;
import com.faforever.client.preferences.SettingsController;
import com.faforever.client.preferences.ui.PreferencesController;
import com.faforever.client.rankedmatch.Ranked1v1Controller;
import com.faforever.client.replay.ReplayVaultController;
import com.faforever.client.units.UnitsController;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

@org.springframework.context.annotation.Configuration
@Import(BaseConfig.class)
public class UiConfig {

  @Resource
  Environment environment;

  @Resource
  BaseConfig baseConfig;

  @Bean
  OnGameFullNotifier onGameFullFaWindowFlasher() {
    return new OnGameFullNotifier();
  }

  @Bean
  FriendJoinedGameNotifier friendJoinedGameNotifier() {
    return new FriendJoinedGameNotifier();
  }

  @Bean
  AchievementUnlockedNotifier achievementUnlockedNotifier() {
    return new AchievementUnlockedNotifier();
  }

  // TODO this seems obsolete, remove it
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
  UserMenuController userMenuController() {
    return loadController("user_menu.fxml");
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
  Ranked1v1Controller ranked1v1Controller() {
    return loadController("ranked_1v1.fxml");
  }

  @Bean
  MapDetailController mapDetailController() {
    return loadController("map_detail.fxml");
  }

  @Bean
  ChatController chatController() {
    return loadController("chat.fxml");
  }

  @Bean
  UnitsController unitsController() {
    return loadController("units.fxml");
  }

  @Bean
  LeaderboardController leaderboardController() {
    return loadController("leaderboard.fxml");
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
  WindowController windowController() {
    return loadController("window.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  FilterUserController filterUserController() {
    return loadController("filter_user.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ModTileController modTileController() {
    return loadController("mod_tile.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  MapTileController mapTileController() {
    return loadController("map_tile.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  GameStatusTooltipController gameStatusContainerTooltipController() {
    return loadController("game_status_tooltip.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  GamesTableController gameTableController() {
    return loadController("games_table.fxml");
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
  GameTooltipController gameContainerTooltipController() {
    return loadController("game_tooltip.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  GamesTilesContainerController gamesTiledController() {
    return loadController("games_tiles_container.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  UserInfoWindowController userInfoWindowController() {
    return loadController("user_info_window.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  GameTileController gameCardController() {
    return loadController("game_tile.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  AchievementItemController achievementItemController() {
    return loadController("achievement_item.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  NewsListItemController newsListItemController() {
    return loadController("news_list_item.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  PersistentNotificationController persistentNotificationController() {
    return loadController("persistent_notification.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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
  ModDetailController modDetailController() {
    return loadController("mod_detail.fxml");
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
  PreferencesController settingsWindowController() {
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

  @Bean
  TransientNotificationsController transientNotificationsController() {
    return loadController("transient_notifications.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  TransientNotificationController transientNotificationController() {
    return loadController("transient_notification.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ChatUserItemController chatUserItemController() {
    return loadController("chat_user_item.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ModUploadController uploadModController() {
    return loadController("mod_upload.fxml");
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  MapUploadController uploadMapController() {
    return loadController("map_upload.fxml");
  }

  @Bean
  JoinGameHelper joinGameHelper() {
    return new JoinGameHelper();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  AutoCompletionHelper autoCompletitionHelper() {
    return new AutoCompletionHelper();
  }
}
