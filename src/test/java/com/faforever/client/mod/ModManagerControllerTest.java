package com.faforever.client.mod;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.mod.ModVersion.ModType;
import com.faforever.client.test.UITest;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModManagerControllerTest extends UITest {

  private ModManagerController instance;
  @Mock
  private ModService modService;
  private ModVersion modSIM;
  private ModVersion modUI;

  @BeforeEach
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
    JavaFxUtil.runLater(() -> {
      instance.viewToggleGroup.selectToggle(instance.uiModsButton);
      instance.uiModsButton.fireEvent(new ActionEvent());
    });
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.modListView.getItems(), Matchers.contains(modUI));
    assertThat(instance.modListView.getSelectionModel().getSelectedItems(), Matchers.contains(modUI));

    JavaFxUtil.runLater(() -> {
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