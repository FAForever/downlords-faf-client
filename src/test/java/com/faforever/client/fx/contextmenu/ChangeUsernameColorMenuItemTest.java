package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChangeUsernameColorMenuItemTest extends PlatformTest {


  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;
  @Spy
  private ChatPrefs chatPrefs;

  private ChangeUsernameColorMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new ChangeUsernameColorMenuItem(uiService, i18n, contextMenuBuilder, chatPrefs);
    chatPrefs.setChatColorMode(ChatColorMode.DEFAULT);
  }

  @Test
  public void testVisibleItem() {
    runOnFxThreadAndWait(
        () -> instance.setObject(ChatChannelUserBuilder.create("junit", new ChatChannel("channel")).get()));
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenChatColorModeIsRandom() {
    chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
    runOnFxThreadAndWait(() -> instance.setObject(new ChatChannelUser("test", new ChatChannel("test"))));
    assertFalse(instance.isVisible());
  }
}