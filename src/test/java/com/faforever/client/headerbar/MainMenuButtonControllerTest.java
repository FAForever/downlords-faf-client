package com.faforever.client.headerbar;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.ui.SettingsController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.IOException;
import java.nio.file.Path;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainMenuButtonControllerTest extends PlatformTest {

  @Mock
  private PlatformService platformService;
  @Mock
  private SettingsController settingsController;
  @Mock
  private UiService uiService;
  @Spy
  private OperatingSystem operatingSystem = new OsPosix();
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Spy
  private DataPrefs dataPrefs;

  @InjectMocks
  private MainMenuButtonController instance;

  @BeforeEach
  public void setup() throws IOException, InterruptedException {
    Path cwd = Path.of(".");
    forgedAlliancePrefs.setVaultBaseDirectory(cwd);
    dataPrefs.setBaseDataDirectory(cwd);

    when(uiService.loadFxml("theme/settings/settings.fxml")).thenReturn(settingsController);
    when(settingsController.getRoot()).thenReturn(new Region());

    loadFxml("theme/headerbar/main_menu_button.fxml", clazz -> instance);
  }

  @Test
  public void testOnRevealMapFolder() throws Exception {
    instance.onRevealMapFolder();
    verify(platformService).reveal(forgedAlliancePrefs.getMapsDirectory());
  }

  @Test
  public void testOnRevealModFolder() throws Exception {
    instance.onRevealModFolder();
    verify(platformService).reveal(forgedAlliancePrefs.getModsDirectory());
  }

  @Test
  public void testOnRevealLogFolder() throws Exception {
    instance.onRevealLogFolder();
    verify(platformService).reveal(operatingSystem.getLoggingDirectory());
  }

  @Test
  public void testOnRevealReplayFolder() throws Exception {
    instance.onRevealReplayFolder();
    verify(platformService).reveal(dataPrefs.getReplaysDirectory());
  }
}
