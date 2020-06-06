package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import lombok.Setter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.update4j.service.UpdateHandler;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.prefs.Preferences;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ClientUpdateTask extends CompletableTask<Void> {

  private final I18n i18n;
  private final PreferencesService preferencesService;

  @Setter
  private UpdateInfo updateInfo;

  @Inject
  public ClientUpdateTask(I18n i18n, PreferencesService preferencesService) {
    super(Priority.MEDIUM);

    this.i18n = i18n;
    this.preferencesService = preferencesService;
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("clientUpdateDownloadTask.title"));

    // update4j will check for an .update subdirectory
    Path updateDirectory = preferencesService.getCacheDirectory();

    updateInfo.getConfiguration().updateTemp(updateDirectory, updateHandler());

    Preferences preferences = Preferences.userRoot().node("/com/faforever/client");
    preferences.put("updateDir", updateDirectory.toAbsolutePath().toString());

    return null;
  }

  private UpdateHandler updateHandler() {
    return new UpdateHandler() {
      @Override
      public void updateDownloadProgress(float frac) {
        updateProgress(frac, 1);
      }
    };
  }

}
