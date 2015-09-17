package com.faforever.client.chat;

import com.faforever.client.fx.HostService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import javafx.stage.Stage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelTabControllerTest extends AbstractPlainJavaFxTest {

  private static final String CHANNEL_NAME = "#testChannel";

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();
  @Mock
  ChatService chatService;
  @Mock
  ChannelTabController instance;
  @Mock
  UserService userService;
  @Mock
  ImageUploadService imageUploadService;
  @Mock
  PlayerService playerService;
  @Mock
  TimeService timeService;
  @Mock
  PreferencesService preferencesService;
  @Mock
  HostService hostService;
  @Mock
  Preferences preferences;
  @Mock
  ChatPrefs chatPrefs;

  @Override
  public void start(Stage stage) throws Exception {
    super.start(stage);

    instance = loadController("channel_tab.fxml");
    instance.chatService = chatService;
    instance.userService = userService;
    instance.imageUploadService = imageUploadService;
    instance.playerService = playerService;
    instance.timeService = timeService;
    instance.preferencesService = preferencesService;
    instance.hostService = hostService;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getCacheDirectory()).thenReturn(tempDir.getRoot().toPath());
    when(preferences.getTheme()).thenReturn("default");
    when(preferences.getChatPrefs()).thenReturn(chatPrefs);
    when(chatPrefs.getZoom()).thenReturn(1d);
    when(userService.getUsername()).thenReturn("junit");

    instance.postConstruct();
  }

  @Test
  public void testGetMessagesWebView() throws Exception {
    assertNotNull(instance.getMessagesWebView());
  }

  @Test
  public void testGetMessageTextField() throws Exception {
    assertNotNull(instance.getMessageTextField());
  }

  @Test
  public void testSetChannelName() throws Exception {
    instance.setChannelName(CHANNEL_NAME);

    verify(chatService).addChannelUserListListener(eq(CHANNEL_NAME), any());
  }
}
