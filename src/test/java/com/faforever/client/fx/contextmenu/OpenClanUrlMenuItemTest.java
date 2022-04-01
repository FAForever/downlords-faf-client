package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.ClanBeanBuilder;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class OpenClanUrlMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;

  @InjectMocks
  private OpenClanUrlMenuItem instance;

  @Test
  public void testOpenClanUrl() {
    instance.setObject(ClanBeanBuilder.create().defaultValues().websiteUrl("https://site.com").get());
    instance.onClicked();
    verify(platformService).showDocument("https://site.com");
  }

  @Test
  public void testVisibleItem() {
    instance.setObject(ClanBeanBuilder.create().defaultValues().websiteUrl("https://site.com").get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenNoClan() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenNoClanUrl() {
    instance.setObject(ClanBeanBuilder.create().defaultValues().websiteUrl("").get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testGetItemText() {
    instance.getItemText();
    verify(i18n).get(any());
  }
}