package com.faforever.client.config;

import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.MockChatService;
import com.faforever.client.chat.PircBotXChatService;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fa.ForgedAllianceServiceImpl;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameServiceImpl;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.leaderboard.LeaderboardServiceImpl;
import com.faforever.client.leaderboard.MockLeaderboardService;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.LobbyServerAccessorImpl;
import com.faforever.client.legacy.MockLobbyServerAccessor;
import com.faforever.client.legacy.MockStatisticsServerAccessor;
import com.faforever.client.legacy.StatisticsServerAccessor;
import com.faforever.client.legacy.StatisticsServerAccessorImpl;
import com.faforever.client.legacy.htmlparser.HtmlParser;
import com.faforever.client.legacy.ladder.LeaderParser;
import com.faforever.client.legacy.ladder.LegacyLeaderParser;
import com.faforever.client.legacy.map.LegacyMapVaultParser;
import com.faforever.client.legacy.map.MapVaultParser;
import com.faforever.client.legacy.proxy.Proxy;
import com.faforever.client.legacy.proxy.ProxyImpl;
import com.faforever.client.legacy.relay.LocalRelayServer;
import com.faforever.client.legacy.relay.LocalRelayServerImpl;
import com.faforever.client.lobby.LobbyService;
import com.faforever.client.lobby.LobbyServiceImpl;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl;
import com.faforever.client.mod.ModService;
import com.faforever.client.mod.ModServiceImpl;
import com.faforever.client.network.DownlordsPortCheckServiceImpl;
import com.faforever.client.network.PortCheckService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.NotificationServiceImpl;
import com.faforever.client.patch.GitRepositoryPatchService;
import com.faforever.client.patch.GitWrapper;
import com.faforever.client.patch.JGitWrapper;
import com.faforever.client.patch.PatchService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.PlayerServiceImpl;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayFileReader;
import com.faforever.client.replay.ReplayFileReaderImpl;
import com.faforever.client.replay.ReplayFileWriter;
import com.faforever.client.replay.ReplayFileWriterImpl;
import com.faforever.client.replay.ReplayServer;
import com.faforever.client.replay.ReplayServerAccessor;
import com.faforever.client.replay.ReplayServerAccessorImpl;
import com.faforever.client.replay.ReplayServerImpl;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.replay.ReplayServiceImpl;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.reporting.ReportingServiceImpl;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.stats.StatisticsServiceImpl;
import com.faforever.client.task.TaskService;
import com.faforever.client.task.TaskServiceImpl;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.uploader.imgur.ImgurImageUploadService;
import com.faforever.client.upnp.UpnpService;
import com.faforever.client.upnp.WeUpnpServiceImpl;
import com.faforever.client.user.UserService;
import com.faforever.client.user.UserServiceImpl;
import com.faforever.client.util.TimeService;
import com.faforever.client.util.TimeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

@org.springframework.context.annotation.Configuration
@Import(BaseConfig.class)
public class ServiceConfig {

  @Autowired
  Environment environment;

  @Bean
  HtmlParser htmlParser() {
    return new HtmlParser();
  }

  @Bean
  UserService userService() {
    return new UserServiceImpl();
  }

  @Bean
  LobbyServerAccessor lobbyServerAccessor() {
    if (environment.containsProperty("faf.testing")) {
      return new MockLobbyServerAccessor();
    }
    return new LobbyServerAccessorImpl();
  }

  @Bean
  StatisticsServerAccessor statisticsServerAccessor() {
    if (environment.containsProperty("faf.testing")) {
      return new MockStatisticsServerAccessor();
    }
    return new StatisticsServerAccessorImpl();
  }

  @Bean
  ReplayServerAccessor replayServerAccessor() {
    return new ReplayServerAccessorImpl();
  }

  @Bean
  ChatService chatService() {
    if (environment.containsProperty("faf.testing")) {
      return new MockChatService();
    }
    return new PircBotXChatService();
  }

  @Bean
  MapVaultParser mapVaultParser(){ return new LegacyMapVaultParser();}

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
    return new DownlordsPortCheckServiceImpl();
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
  LeaderParser ladderParser() {
    return new LegacyLeaderParser();
  }

  @Bean
  ReplayServer replayServer() {
    return new ReplayServerImpl();
  }

  @Bean
  ReplayFileWriter replayFileWriter() {
    return new ReplayFileWriterImpl();
  }

  @Bean
  ReplayFileReader replayFileReader() {
    return new ReplayFileReaderImpl();
  }

  @Bean
  UpnpService upnpService() {
    return new WeUpnpServiceImpl();
  }

  @Bean
  LeaderboardService leaderboardService() {
    if (environment.containsProperty("faf.testing")) {
      return new MockLeaderboardService();
    }
    return new LeaderboardServiceImpl();
  }

  @Bean
  Proxy proxy() {
    return new ProxyImpl();
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

  @Bean
  StatisticsService statisticsService() {
    return new StatisticsServiceImpl();
  }

  @Bean
  ReplayService replayService() {
    return new ReplayServiceImpl();
  }

  @Bean
  MapService mapService() {
    return new MapServiceImpl();
  }

  @Bean
  TimeService prettyTime() {
    return new TimeServiceImpl();
  }

  @Bean
  ReportingService reportingService() {
    return new ReportingServiceImpl();
  }

  @Bean
  ImageUploadService imageUploadService() {
    return new ImgurImageUploadService();
  }

  @Bean
  GitWrapper gitWrapper() {
    return new JGitWrapper();
  }
}
