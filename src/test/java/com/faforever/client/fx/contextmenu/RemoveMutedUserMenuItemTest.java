package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemoveMutedUserMenuItemTest extends PlatformTest {

  @Mock
  private I18n i18n;

  private ChatPrefs chatPrefs;
  private RemoveMutedUserMenuItem instance;
  private ChatChannelUser chatUser;

  @BeforeEach
  public void setUp() {
    chatPrefs = new ChatPrefs();
    instance = new RemoveMutedUserMenuItem(chatPrefs, i18n);
    chatUser = new ChatChannelUser("junit", new ChatChannel("#test"));
  }

  @Test
  public void testRemoveMutedUser() {
    chatPrefs.addMutedUser(chatUser.getUsername());

    instance.setObject(chatUser);
    instance.onClicked();

    assertFalse(chatPrefs.isUserMuted(chatUser.getUsername()));
  }

  @Test
  public void testVisibleItemIfUserIsMuted() {
    chatPrefs.addMutedUser(chatUser.getUsername());

    instance.setObject(chatUser);

    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUserIsNotMuted() {
    instance.setObject(chatUser);

    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUserIsSelf() {
    chatPrefs.addMutedUser(chatUser.getUsername());
    chatUser.setPlayer(PlayerInfoBuilder.create().defaultValues().socialStatus(SocialStatus.SELF).get());

    instance.setObject(chatUser);

    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoUser() {
    instance.setObject(null);

    assertFalse(instance.isVisible());
  }
}
