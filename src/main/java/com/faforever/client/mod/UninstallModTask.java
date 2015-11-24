package com.faforever.client.mod;

import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Objects;

public class UninstallModTask extends AbstractPrioritizedTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  ModService modService;

  private ModInfoBean mod;

  public UninstallModTask() {
    super(AbstractPrioritizedTask.Priority.LOW);
  }

  public void setMod(ModInfoBean mod) {
    this.mod = mod;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(mod, "mod has not been set");

    logger.info("Uninstalling mod '{}' ({})", mod.getName(), mod.getId());
    Path modPath = modService.getPathForMod(mod);

    ResourceLocks.acquireDiskLock();
    try {
      FileUtils.deleteRecursively(modPath);
    } finally {
      ResourceLocks.freeDiskLock();
    }

    return null;
  }
}
