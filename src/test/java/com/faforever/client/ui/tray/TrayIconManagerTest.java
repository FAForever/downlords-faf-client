package com.faforever.client.ui.tray;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class TrayIconManagerTest extends UITest {

  @TempDir
  public Path temporaryFolder;

  private TrayIconManager instance;

  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new TrayIconManager(i18n, eventBus);

    when(i18n.number(anyInt())).thenReturn("1");
  }

  @Test
  public void onSetApplicationBadgeEventOfNewValue() throws Exception {
    instance.onSetApplicationBadgeEvent(UpdateApplicationBadgeEvent.ofNewValue(1));
    WaitForAsyncUtils.waitForFxEvents();

    getStage().getIcons().forEach(image -> JavaFxUtil.persistImage(image, temporaryFolder.resolve((int) image.getWidth() + ".png"), "png"));
    assertThat(getStage().getIcons(), hasSize(5));
  }

  @Test
  public void onSetApplicationBadgeEventOfDelta() throws Exception {
    instance.onSetApplicationBadgeEvent(UpdateApplicationBadgeEvent.ofDelta(1));
    WaitForAsyncUtils.waitForFxEvents();

    getStage().getIcons().forEach(image -> JavaFxUtil.persistImage(image, temporaryFolder.resolve((int) image.getWidth() + ".png"), "png"));
    assertThat(getStage().getIcons(), hasSize(5));
  }
}
