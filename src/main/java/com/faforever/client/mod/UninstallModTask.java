package com.faforever.client.mod;

import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.io.FileUtils;
import com.faforever.client.task.CompletableTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class UninstallModTask extends CompletableTask<Void> {

  private final ModService modService;

  private ModVersionBean modVersion;

  @Inject
  public UninstallModTask(ModService modService) {
    super(CompletableTask.Priority.LOW);

    this.modService = modService;
  }

  public void setModVersion(ModVersionBean modVersion) {
    this.modVersion = modVersion;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(modVersion, "modVersion has not been set");

    log.info("Uninstalling modVersion '{}' ({})", modVersion.getMod().getDisplayName(), modVersion.getUid());
    Path modPath = modService.getPathForMod(modVersion);

    FileUtils.deleteRecursively(modPath);

    return null;
  }
}
