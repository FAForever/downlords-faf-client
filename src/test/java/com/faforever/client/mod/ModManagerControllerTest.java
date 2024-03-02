package com.faforever.client.mod;

import com.faforever.client.domain.api.ModType;
import com.faforever.client.domain.api.ModVersion;
import com.faforever.client.test.PlatformTest;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import org.hamcrest.Matchers;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModManagerControllerTest extends PlatformTest {

  @InjectMocks
  private ModManagerController instance;
  @Mock
  private ModService modService;
  private ModVersion modSIM;
  private ModVersion modUI;

  @BeforeEach
  public void setUp() throws Exception {
    modUI = Instancio.of(ModVersion.class).set(field(ModVersion::modType), ModType.UI).create();
    modSIM = Instancio.of(ModVersion.class).set(field(ModVersion::modType), ModType.SIM).create();
    when(modService.getInstalledMods()).thenReturn(FXCollections.observableArrayList(modUI, modSIM));

    when(modService.getActivatedSimAndUIMods()).thenReturn(Collections.singletonList(modUI));

    loadFxml("theme/mod_manager.fxml", param -> instance);
  }

  @Test
  public void testCorrectModsSelected() {
    runOnFxThreadAndWait(() -> {
      instance.viewToggleGroup.selectToggle(instance.uiModsButton);
      instance.uiModsButton.fireEvent(new ActionEvent());
    });
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.modListView.getItems(), Matchers.contains(modUI));
    assertThat(instance.modListView.getSelectionModel().getSelectedItems(), Matchers.contains(modUI));

    runOnFxThreadAndWait(() -> {
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

    verify(modService).overrideActivatedMods(ArgumentMatchers.eq(Collections.singleton(modUI)));
  }
}