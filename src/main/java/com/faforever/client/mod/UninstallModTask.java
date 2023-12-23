package com.faforever.client.mod;

import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.task.PrioritizedCompletableTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Path;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class UninstallModTask extends PrioritizedCompletableTask<Void> {

  private final ModService modService;

  private ModVersionBean modVersion;

  @Autowired
  public UninstallModTask(ModService modService) {
    super(PrioritizedCompletableTask.Priority.LOW);

    this.modService = modService;
  }

  public void setMod(ModVersionBean modVersion) {
    this.modVersion = modVersion;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(modVersion, "modVersion has not been set");

    log.info("Uninstalling modVersion '{}' ({})", modVersion.getMod().getDisplayName(), modVersion.getUid());
    Path modPath = modService.getPathForMod(modVersion);

    FileSystemUtils.deleteRecursively(modPath);

    return null;
  }
}
