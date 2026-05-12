package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

public class UnmuteChatUserMenuItemTest extends PlatformTest {

  @Mock
  private ChatService chatService;
  @Mock
  private I18n i18n;

  private ChatPrefs chatPrefs;
  private UnmuteChatUserMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    chatPrefs = new ChatPrefs();
    instance = new UnmuteChatUserMenuItem(chatPrefs, chatService, i18n);
    lenient().when(chatService.getCurrentUsername()).thenReturn("junit");
  }

  @Test
  public void testUnmuteUser() {
    ChatChannelUser chatUser = new ChatChannelUser("other", new ChatChannel("#test"));
    chatPrefs.muteUser("other");

    instance.setObject(chatUser);
    instance.onClicked();

    assertFalse(chatPrefs.isUserMuted("other"));
  }

  @Test
  public void testVisibleItemIfUserIsMuted() {
    chatPrefs.muteUser("other");

    instance.setObject(new ChatChannelUser("other", new ChatChannel("#test")));

    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUserIsNotMuted() {
    instance.setObject(new ChatChannelUser("other", new ChatChannel("#test")));

    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUserIsCurrentUser() {
    chatPrefs.muteUser("junit");

    instance.setObject(new ChatChannelUser("junit", new ChatChannel("#test")));

    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoUser() {
    instance.setObject(null);

    assertFalse(instance.isVisible());
  }
}
