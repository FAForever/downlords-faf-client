package com.faforever.client.fx;

import com.faforever.client.ThemeService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StageConfiguratorImplTest extends AbstractPlainJavaFxTest {

  private StageConfiguratorImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new StageConfiguratorImpl();
    instance.fxmlLoader = mock(FxmlLoader.class);
    instance.preferencesService = mock(PreferencesService.class);
    instance.themeService = mock(ThemeService.class);


    doAnswer(invocation -> getThemeFile(invocation.getArgumentAt(0, String.class)))
        .when(instance.themeService).getThemeFile(any());

    when(instance.preferencesService.getPreferences()).thenReturn(mock(Preferences.class));
  }

  @Test
  public void testConfigureScene() throws Exception {
    Pane rootPane = new Pane();

    WindowDecorator windowDecorator = mock(WindowDecorator.class);
    when(windowDecorator.getWindowRoot()).thenReturn(rootPane);
    when(instance.fxmlLoader.loadAndGetController("window.fxml")).thenReturn(windowDecorator);
    when(instance.themeService.getThemeFile("style.css")).thenReturn("");

    CountDownLatch serviceStateDoneFuture = new CountDownLatch(1);
    WaitForAsyncUtils.waitForAsyncFx(3000, () -> {
      instance.configureScene(getStage(), getRoot(), false);
      serviceStateDoneFuture.countDown();
    });

    serviceStateDoneFuture.await(3000, TimeUnit.MILLISECONDS);

    assertThat(getStage().isResizable(), is(false));
    assertThat(getStage().getScene().getRoot(), is(rootPane));
  }
}
