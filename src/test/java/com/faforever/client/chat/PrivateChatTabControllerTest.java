package com.faforever.client.chat;

import com.faforever.client.audio.AudioController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.time.Instant;

import static org.mockito.Mockito.when;

public class PrivateChatTabControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  PreferencesService preferencesService;
  @Mock
  Preferences preferences;
  @Mock
  ChatPrefs chatPrefs;
  @Mock
  PlayerService playerService;
  @Mock
  AudioController audioController;

  private PrivateChatTabController instance;
  private PlayerInfoBean playerInfoBean;
  private String playerName;

  @Before
  public void setUp() throws IOException {
    instance = loadController("private_chat_tab.fxml");
    instance.preferencesService = preferencesService;
    instance.playerService = playerService;
    instance.audioController = audioController;

    playerName = "testUser";
    playerInfoBean = new PlayerInfoBean(playerName);

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getChat()).thenReturn(chatPrefs);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(playerInfoBean);
  }

  @Test
  public void onChatMessageTestNotFoeShowFoe() {
    when(chatPrefs.getHideFoeMessages()).thenReturn(false);
    instance.onChatMessage(new ChatMessage(Instant.now(), playerName, "Test message"));
  }

  @Ignore("Not yet implemented")
  @Test
  public void onChatMessageTestNotFoeHideFoe() {

  }

  @Ignore("Not yet implemented")
  @Test
  public void onChatMessageTestIsFoeShowFoe() {

  }

  @Ignore("Not yet implemented")
  @Test
  public void onChatMessageTestIsFoeHideFoe() {

  }
}
