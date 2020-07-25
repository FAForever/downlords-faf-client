package com.faforever.client.mod;

import com.faforever.client.mod.ModVersion.ModType;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModManagerControllerTest extends AbstractPlainJavaFxTest {

  private ModManagerController instance;
  @Mock
  private ModService modService;
  private ModVersion modSIM;
  private ModVersion modUI;

  @Before
  public void setUp() throws Exception {
    instance = new ModManagerController(modService);

    modUI = new ModVersion();
    modSIM = new ModVersion();
    modUI.setUid("UI");
    modSIM.setUid("SIM");
    modUI.setModType(ModType.UI);
    modSIM.setModType(ModType.SIM);
    when(modService.getInstalledModVersions()).thenReturn(FXCollections.observableArrayList(modUI, modSIM));

    when(modService.getActivatedSimAndUIMods()).thenReturn(Collections.singletonList(modUI));

    loadFxml("theme/mod_manager.fxml", param -> instance);
  }

  @Test
  public void testCorrectModsSelected() {
    Platform.runLater(() -> {
      instance.viewToggleGroup.selectToggle(instance.uiModsButton);
      instance.uiModsButton.fireEvent(new ActionEvent());
    });
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.modListView.getItems(), Matchers.contains(modUI));
    assertThat(instance.modListView.getSelectionModel().getSelectedItems(), Matchers.contains(modUI));

    Platform.runLater(() -> {
      instance.viewToggleGroup.selectToggle(instance.simModsButton);
      instance.simModsButton.fireEvent(new ActionEvent());
    });
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.modListView.getItems(), Matchers.contains(modSIM));
    assertThat(instance.modListView.getSelectionModel().getSelectedItems().isEmpty(), Matchers.is(true));
  }

  @Test
  public void testApplyCallsModService() throws IOException {
    instance.apply();

    verify(modService).overrideActivatedMods(ArgumentMatchers.eq(Collections.singletonList(modUI)));
  }
}