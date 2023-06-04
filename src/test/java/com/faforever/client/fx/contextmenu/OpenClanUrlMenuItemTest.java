package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.ClanBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.clan.ClanService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenClanUrlMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private ClanService clanService;

  private OpenClanUrlMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new OpenClanUrlMenuItem(i18n, platformService, clanService);
  }

  @Test
  public void testOpenClanUrl() {
    when(clanService.getClanByTag(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.of(ClanBeanBuilder.create()
        .defaultValues()
        .websiteUrl("https://site.com")
        .get())));

    instance.setObject(PlayerBeanBuilder.create().clan("clan").get());
    instance.onClicked();
    verify(platformService).showDocument("https://site.com");
  }

  @Test
  public void testVisibleItem() {
    instance.setObject(PlayerBeanBuilder.create().clan("clan").get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenNoClan() {
    instance.setObject(PlayerBeanBuilder.create().get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testGetItemText() {
    instance.getItemText();
    verify(i18n).get(any());
  }
}