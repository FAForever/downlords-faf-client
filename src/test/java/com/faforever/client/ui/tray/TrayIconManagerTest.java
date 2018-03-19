package com.faforever.client.ui.tray;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class TrayIconManagerTest extends AbstractPlainJavaFxTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private TrayIconManager instance;

  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;

  @Before
  public void setUp() throws Exception {
    instance = new TrayIconManager(i18n, eventBus);

    when(i18n.number(anyInt())).thenReturn("1");
  }

  @Test
  public void onSetApplicationBadgeEventOfNewValue() throws Exception {
    instance.onSetApplicationBadgeEvent(UpdateApplicationBadgeEvent.ofNewValue(1));
    WaitForAsyncUtils.waitForFxEvents();

    Path tmpDir = temporaryFolder.getRoot().toPath();

    getStage().getIcons().forEach(image -> JavaFxUtil.persistImage(image, tmpDir.resolve((int) image.getWidth() + ".png"), "png"));
    assertThat(getStage().getIcons(), hasSize(5));
  }

  @Test
  public void onSetApplicationBadgeEventOfDelta() throws Exception {
    instance.onSetApplicationBadgeEvent(UpdateApplicationBadgeEvent.ofDelta(1));
    WaitForAsyncUtils.waitForFxEvents();

    Path tmpDir = temporaryFolder.getRoot().toPath();

    getStage().getIcons().forEach(image -> JavaFxUtil.persistImage(image, tmpDir.resolve((int) image.getWidth() + ".png"), "png"));
    assertThat(getStage().getIcons(), hasSize(5));
  }
}
