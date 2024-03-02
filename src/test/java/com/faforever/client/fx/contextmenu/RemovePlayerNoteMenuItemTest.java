package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.social.SocialService;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

public class RemovePlayerNoteMenuItemTest extends PlatformTest {

  @Mock
  private PlayerService playerService;
  @Mock
  private SocialService socialService;
  @Mock
  private I18n i18n;

  private RemovePlayerNoteMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new RemovePlayerNoteMenuItem(playerService, socialService, i18n);
  }

  @Test
  public void testOnItemClicked() {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().note("junit").get();
    instance.setObject(player);
    instance.onClicked();
    verify(socialService).removeNote(player);
  }

  @Test
  public void testVisibleItem() {
    instance.setObject(PlayerInfoBuilder.create().defaultValues().note("junit").get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoteIsBlank() {
    instance.setObject(PlayerInfoBuilder.create().defaultValues().get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfOwnPlayer() {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();

    instance.setObject(player);
    assertFalse(instance.isVisible());
  }
}