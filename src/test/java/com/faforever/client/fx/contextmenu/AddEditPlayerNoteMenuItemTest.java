package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class AddEditPlayerNoteMenuItemTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;

  private AddEditPlayerNoteMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new AddEditPlayerNoteMenuItem(uiService, playerService, i18n);
  }

  @Test
  public void testAddNoteText() {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
    when(i18n.get("chat.userContext.addNote")).thenReturn("add");

    instance.setObject(player);
    assertEquals("add", instance.getText());
  }

  @Test
  public void testEditNoteText() {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().note("junit").get();
    when(i18n.get("chat.userContext.editNote")).thenReturn("edit");

    instance.setObject(player);
    assertEquals("edit", instance.getText());
  }

  @Test
  public void testVisibleItem() {
    instance.setObject(PlayerInfoBuilder.create().defaultValues().get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItem() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfOwnPlayer() {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(player);

    instance.setObject(player);
    assertFalse(instance.isVisible());
  }
}