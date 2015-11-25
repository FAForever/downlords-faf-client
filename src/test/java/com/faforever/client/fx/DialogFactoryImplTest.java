package com.faforever.client.fx;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import org.junit.Before;
import org.junit.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DialogFactoryImplTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 5000;
  private DialogFactoryImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new DialogFactoryImpl();
    instance.fxmlLoader = mock(FxmlLoader.class);
    instance.preferencesService = mock(PreferencesService.class);

    when(instance.preferencesService.getPreferences()).thenReturn(mock(Preferences.class));
  }

  @Test
  public void testCreateAlert() throws Exception {
    DialogPane pane = new DialogPane();
    when(instance.fxmlLoader.loadAndGetRoot("dialog.fxml")).thenReturn(pane);

    CompletableFuture<Alert> serviceStateDoneFuture = new CompletableFuture<>();
    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> {
      Alert alert = instance.createAlert(Alert.AlertType.CONFIRMATION, "text");
      serviceStateDoneFuture.complete(alert);
    });

    Alert alert = serviceStateDoneFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);

    assertNotNull(alert);
    assertEquals(pane, alert.getDialogPane());
  }
}
