package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
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
import static org.mockito.Mockito.when;

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
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().note("junit").get();
    instance.setObject(player);
    instance.onClicked();
    verify(socialService).removeNote(player);
  }

  @Test
  public void testVisibleItem() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().note("junit").get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoteIsBlank() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfOwnPlayer() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(player);

    instance.setObject(player);
    assertFalse(instance.isVisible());
  }
}