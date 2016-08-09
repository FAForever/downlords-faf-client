package com.faforever.client.map;

import com.faforever.client.io.FileUtils;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.ResourceLocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Objects;

public class UninstallMapTask extends AbstractPrioritizedTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  MapService mapService;

  private MapBean map;

  public UninstallMapTask() {
    super(Priority.LOW);
  }

  public void setMap(MapBean map) {
    this.map = map;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(map, "map has not been set");

    logger.info("Uninstalling map '{}' ({})", map.getFolderName(), map.getId());
    Path mapPath = mapService.getPathForMap(map);

    ResourceLocks.acquireDiskLock();
    try {
      FileUtils.deleteRecursively(mapPath);
    } finally {
      ResourceLocks.freeDiskLock();
    }

    return null;
  }
}
