package com.faforever.client.config;

import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.MockChatService;
import com.faforever.client.chat.PircBotXChatService;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameServiceImpl;
import com.faforever.client.i18n.I18n;
import com.faforever.client.i18n.I18nImpl;
import com.faforever.client.leaderboard.LadderService;
import com.faforever.client.leaderboard.LadderServiceImpl;
import com.faforever.client.leaderboard.MockLadderService;
import com.faforever.client.legacy.MockServerAccessor;
import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.legacy.ServerAccessorImpl;
import com.faforever.client.legacy.htmlparser.HtmlParser;
import com.faforever.client.legacy.ladder.LadderParser;
import com.faforever.client.legacy.ladder.LegacyLadderParser;
import com.faforever.client.legacy.map.LegacyMapVaultParser;
import com.faforever.client.legacy.map.MapVaultParser;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.legacy.proxy.ProxyImpl;
import com.faforever.client.legacy.relay.LocalRelayServer;
import com.faforever.client.legacy.relay.LocalRelayServerImpl;
import com.faforever.client.lobby.LobbyService;
import com.faforever.client.lobby.LobbyServiceImpl;
import com.faforever.client.mod.ModService;
import com.faforever.client.mod.ModServiceImpl;
import com.faforever.client.network.PortCheckService;
import com.faforever.client.network.PortCheckServiceImpl;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.NotificationServiceImpl;
import com.faforever.client.patch.GitRepositoryPatchService;
import com.faforever.client.patch.PatchService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.PlayerServiceImpl;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.supcom.ForgedAllianceService;
import com.faforever.client.supcom.ForgedAllianceServiceImpl;
import com.faforever.client.task.TaskService;
import com.faforever.client.task.TaskServiceImpl;
import com.faforever.client.user.UserService;
import com.faforever.client.user.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.Locale;

/**
 * This configuration has to be imported by other configurations and should only contain beans that are necessary to run
 * the application, whether the user is logged in or not.
 */
@org.springframework.context.annotation.Configuration
@PropertySource("classpath:/faf_client.properties")
public class BaseConfig {

  private static final java.lang.String PROPERTY_LOCALE = "locale";

  @Autowired
  Environment environment;

  @Bean
  Locale locale() {
    return new Locale(environment.getProperty(PROPERTY_LOCALE));
  }

  @Bean
  ResourceBundleMessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.Messages");
    return messageSource;
  }

  @Bean
  I18n i18n() {
    return new I18nImpl(messageSource(), locale());
  }

  @Bean
  HtmlParser htmlParser() {
    return new HtmlParser();
  }

  @Bean
  UserService userService() {
    return new UserServiceImpl();
  }

  @Bean
  ServerAccessor serverAccessor() {
    if (environment.containsProperty("faf.testing")) {
      return new MockServerAccessor();
    } else {
      return new ServerAccessorImpl();
    }
  }

  @Bean
  ChatService chatService() {
    if (environment.containsProperty("faf.testing")) {
      return new MockChatService();
    } else {
      return new PircBotXChatService();
    }
  }

  @Bean
  GameService gameService() {
    return new GameServiceImpl();
  }

  @Bean
  PlayerService playerService() {
    return new PlayerServiceImpl();
  }

  @Bean
  PreferencesService preferencesService() {
    return new PreferencesService();
  }

  @Bean
  ForgedAllianceService processService() {
    return new ForgedAllianceServiceImpl();
  }

  @Bean
  TaskScheduler taskScheduler() {
    return new ConcurrentTaskScheduler();
  }

  @Bean
  LocalRelayServer relayServer() {
    return new LocalRelayServerImpl();
  }

  @Bean
  PortCheckService portCheckService() {
    return new PortCheckServiceImpl();
  }

  @Bean
  LobbyService lobbyService() {
    return new LobbyServiceImpl();
  }

  @Bean
  ModService modService() {
    return new ModServiceImpl();
  }

  @Bean
  LadderParser ladderParser() {
    return new LegacyLadderParser();
  }

  @Bean
  MapVaultParser mapVaultParser() {
    return new LegacyMapVaultParser();
  }

  @Bean
  LadderService leaderboardService() {
    if (environment.containsProperty("faf.testing")) {
      return new MockLadderService();
    } else {
      return new LadderServiceImpl();
    }
  }

  @Bean
  Proxy proxy() {
    return new ProxyImpl(preferencesService().getPreferences().getForgedAlliance());
  }

  @Bean
  PatchService patchService() {
    return new GitRepositoryPatchService();
  }

  @Bean
  TaskService taskQueueService() {
    return new TaskServiceImpl();
  }

  @Bean
  NotificationService notificationService() {
    return new NotificationServiceImpl();
  }
}
