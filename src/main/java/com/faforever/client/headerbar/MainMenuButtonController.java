package com.faforever.client.headerbar;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.LinksAndHelpController;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.ui.SettingsController;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import javafx.scene.control.MenuButton;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class MainMenuButtonController {

  private final I18n i18n;
  private final UiService uiService;
  private final ThemeService themeService;
  private final PlatformService platformService;
  private final OperatingSystem operatingSystem;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final DataPrefs dataPrefs;

  public MenuButton menuButton;

  public void onRevealMapFolder() {
    Path mapPath = forgedAlliancePrefs.getMapsDirectory();
    this.platformService.reveal(mapPath);
  }

  public void onRevealModFolder() {
    Path modPath = forgedAlliancePrefs.getModsDirectory();
    this.platformService.reveal(modPath);
  }

  public void onRevealLogFolder() {
    this.platformService.reveal(operatingSystem.getLoggingDirectory());
  }

  public void onRevealReplayFolder() {
    this.platformService.reveal(dataPrefs.getReplaysDirectory());
  }

  public void onRevealGamePrefsFolder() {
    this.platformService.reveal(forgedAlliancePrefs.getPreferencesFile());
  }

  public void onRevealDataFolder() {
    this.platformService.reveal(dataPrefs.getBaseDataDirectory());
  }

  public void onExitItemSelected() {
    Stage stage = StageHolder.getStage();
    stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
  }

  public void onSettingsSelected() {
    SettingsController settingsController = uiService.loadFxml("theme/settings/settings.fxml");
    FxStage fxStage = FxStage.create(settingsController.getRoot())
        .initOwner(menuButton.getScene().getWindow())
                             .withSceneFactory(themeService::createScene)
        .allowMinimize(false)
        .apply()
        .setTitleBar(settingsController.settingsHeader);

    Stage stage = fxStage.getStage();

    stage.setTitle(i18n.get("settings.windowTitle"));
    stage.show();
  }

  public void onLinksAndHelp() {
    LinksAndHelpController linksAndHelpController = uiService.loadFxml("theme/links_and_help.fxml");

    FxStage fxStage = FxStage.create(linksAndHelpController.getRoot())
        .initOwner(menuButton.getScene().getWindow())
                             .withSceneFactory(themeService::createScene)
        .allowMinimize(false)
        .apply();

    Stage stage = fxStage.getStage();

    stage.setTitle(i18n.get("help.title"));
    stage.show();
  }
}
