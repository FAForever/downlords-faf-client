package com.faforever.client.chat;

import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
  private PrivateChatTabController instance;
  private PlayerInfoBean playerInfoBean;
  private String playerName;

  @Before
  public void setUp() throws IOException {
    instance = loadController("private_chat_tab.fxml");
    instance.preferencesService = preferencesService;

    playerName = "testUser";
    PlayerInfoBean playerInfoBean = new PlayerInfoBean(playerName);

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getChat()).thenReturn(chatPrefs);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(playerInfoBean);

  }

  @Test
  public void onChatMessageTestNotFoeShowFoe() {
    when(playerInfoBean.getFoe()).thenReturn(false);
    when(chatPrefs.getHideFoeMessages()).thenReturn(false);
    instance.onChatMessage(new ChatMessage(Instant.now(), playerName, "Test message"));
  }

  @Test
  public void onChatMessageTestNotFoeHideFoe() throws NotImplementedException {

  }

  @Test
  public void onChatMessageTestIsFoeShowFoe() throws NotImplementedException {

  }

  @Test
  public void onChatMessageTestIsFoeHideFoe() throws NotImplementedException {

  }
}
