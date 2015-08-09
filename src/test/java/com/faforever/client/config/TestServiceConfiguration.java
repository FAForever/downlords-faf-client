package com.faforever.client.config;

import com.faforever.client.audio.AudioController;
import com.faforever.client.chat.ChatService;
import com.faforever.client.fx.HostService;
import com.faforever.client.game.GameService;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class TestServiceConfiguration {

  @Bean
  UserService userService() {
    UserService mock = mock(UserService.class);
    when(mock.getUsername()).thenReturn("junit");
    return mock;
  }

  @Bean
  NotificationService notificationService() {
    return mock(NotificationService.class);
  }

  @Bean
  ChatService chatService() {
    return mock(ChatService.class);
  }

  @Bean
  HostService hostService() {
    return mock(HostService.class);
  }

  @Bean
  PlayerService playerService() {
    return mock(PlayerService.class);
  }

  @Bean
  AudioController soundService() {
    return mock(AudioController.class);
  }

  @Bean
  TimeService timeService() {
    return mock(TimeService.class);
  }

  @Bean
  ImageUploadService imageUploadService() {
    return mock(ImageUploadService.class);
  }

  @Bean
  GameService gameService() {
    return mock(GameService.class);
  }

  @Bean
  MapService mapService() {
    return mock(MapService.class);
  }

  @Bean
  ModService modService() {
    return mock(ModService.class);
  }

  @Bean
  PreferencesService preferencesService() throws IOException {
    PreferencesService preferencesService = mock(PreferencesService.class);

    ChatPrefs chatPrefs = mock(ChatPrefs.class);
    when(chatPrefs.getZoom()).thenReturn(1d);

    Preferences preferences = mock(Preferences.class);
    when(preferences.getTheme()).thenReturn("default");
    when(preferences.getChat()).thenReturn(chatPrefs);

    Path tempDir = Paths.get("build/tmp");
    Files.createDirectories(tempDir);
    when(preferencesService.getPreferencesDirectory()).thenReturn(Files.createTempDirectory(tempDir, null));
    when(preferencesService.getPreferences()).thenReturn(preferences);

    return preferencesService;
  }
}
