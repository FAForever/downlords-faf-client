package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.clan.ClanService;
import com.faforever.client.domain.api.Clan;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenClanUrlMenuItemTest extends PlatformTest {

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
  public void testOpenClanUrl() throws Exception {
    when(clanService.getClanByTag(anyString())).thenReturn(
        Mono.just(Instancio.of(Clan.class).set(field(Clan::websiteUrl),
                                                                                   URI.create("https://site.com")
                                                                                      .toURL())
                                                                              .create()));

    instance.setObject(PlayerInfoBuilder.create().clan("clan").get());
    instance.onClicked();
    verify(platformService).showDocument("https://site.com");
  }

  @Test
  public void testVisibleItem() {
    instance.setObject(PlayerInfoBuilder.create().clan("clan").get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenNoClan() {
    instance.setObject(PlayerInfoBuilder.create().get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testGetItemText() {
    instance.getItemText();
    verify(i18n).get(any());
  }
}