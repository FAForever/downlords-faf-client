package com.faforever.client.ui.tray;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.ui.tray.event.IncrementApplicationBadgeEvent;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class TrayIconManagerTest extends AbstractPlainJavaFxTest {
  private TrayIconManager instance;

  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;

  @Before
  public void setUp() throws Exception {
    instance = new TrayIconManager(i18n, eventBus);
  }

  @Test
  public void onSetApplicationBadgeEvent() throws Exception {
    instance.onSetApplicationBadgeEvent(new IncrementApplicationBadgeEvent(0));
    WaitForAsyncUtils.waitForFxEvents();

    getStage().getIcons().forEach(image -> JavaFxUtil.persistImage(image, Paths.get((int) image.getWidth() + ".png"), "png"));
    assertThat(getStage().getIcons(), hasSize(5));
  }
}
