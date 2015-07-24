package com.faforever.client.fx;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SceneFactoryImplTest extends AbstractPlainJavaFxTest {

  private SceneFactoryImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new SceneFactoryImpl();
    instance.fxmlLoader = mock(FxmlLoader.class);
    instance.preferencesService = mock(PreferencesService.class);

    when(instance.preferencesService.getPreferences()).thenReturn(mock(Preferences.class));
  }

  @Test
  public void testCreateScene() throws Exception {
    Pane rootPane = new Pane();

    WindowDecorator windowDecorator = mock(WindowDecorator.class);
    when(windowDecorator.getWindowRoot()).thenReturn(rootPane);
    when(instance.fxmlLoader.loadAndGetController("window.fxml")).thenReturn(windowDecorator);

    CompletableFuture<Scene> serviceStateDoneFuture = new CompletableFuture<>();
    WaitForAsyncUtils.waitForAsyncFx(200, () -> {
      Scene scene = instance.createScene(getStage(), getRoot(), false);
      serviceStateDoneFuture.complete(scene);
    });

    Scene scene = serviceStateDoneFuture.get(500, TimeUnit.MILLISECONDS);

    assertNotNull(scene);
    assertEquals(rootPane, scene.getRoot());
  }
}
