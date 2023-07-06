package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShowPlayerInfoMenuItemTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;

  private ShowPlayerInfoMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new ShowPlayerInfoMenuItem(i18n, uiService);
  }

  @Test
  public void testVisibleItemIfNonNullPlayer() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testVisibleItemIfPlayerIsSelf() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.SELF).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}