package com.faforever.client.mod;

import com.faforever.client.io.FileUtils;
import com.faforever.client.task.CompletableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UninstallModTask extends CompletableTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ModService modService;

  private ModVersion modVersion;

  @Inject
  public UninstallModTask(ModService modService) {
    super(CompletableTask.Priority.LOW);

    this.modService = modService;
  }

  public void setModVersion(ModVersion modVersion) {
    this.modVersion = modVersion;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(modVersion, "modVersion has not been set");

    logger.info("Uninstalling modVersion '{}' ({})", modVersion.getDisplayName(), modVersion.getUid());
    Path modPath = modService.getPathForMod(modVersion);

    FileUtils.deleteRecursively(modPath);

    return null;
  }
}
