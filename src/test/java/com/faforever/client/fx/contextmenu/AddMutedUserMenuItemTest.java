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

public class AddMutedUserMenuItemTest extends PlatformTest {

  @Mock
  private I18n i18n;

  private ChatPrefs chatPrefs;
  private AddMutedUserMenuItem instance;
  private ChatChannelUser chatUser;

  @BeforeEach
  public void setUp() {
    chatPrefs = new ChatPrefs();
    instance = new AddMutedUserMenuItem(chatPrefs, i18n);
    chatUser = new ChatChannelUser("junit", new ChatChannel("#test"));
  }

  @Test
  public void testAddMutedUser() {
    instance.setObject(chatUser);
    instance.onClicked();

    assertTrue(chatPrefs.getMutedUsers().contains(chatUser.getUsername()));
  }

  @Test
  public void testVisibleItemIfUserIsNotMuted() {
    instance.setObject(chatUser);

    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUserIsMuted() {
    chatPrefs.getMutedUsers().add(chatUser.getUsername());

    instance.setObject(chatUser);

    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUserIsSelf() {
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
