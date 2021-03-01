package com.faforever.client.map;

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
public class UninstallMapTask extends CompletableTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final MapService mapService;

  private MapBean map;

  @Inject
  public UninstallMapTask(MapService mapService) {
    super(Priority.LOW);
    this.mapService = mapService;
  }

  public void setMap(MapBean map) {
    this.map = map;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(map, "map has not been set");

    logger.info("Uninstalling map '{}'", map.getFolderName());
    Path mapPath = mapService.getPathForMap(map);
    FileUtils.deleteRecursively(mapPath);
    logger.info("Map {} was uninstalled successfully", map.getFolderName());

    return null;
  }
}
