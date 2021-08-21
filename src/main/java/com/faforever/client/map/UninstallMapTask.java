package com.faforever.client.map;

import com.faforever.client.domain.MapVersionBean;
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
public class UninstallMapTask extends CompletableTask<Void> {

  private final MapService mapService;

  private MapVersionBean map;

  @Inject
  public UninstallMapTask(MapService mapService) {
    super(Priority.LOW);
    this.mapService = mapService;
  }

  public void setMap(MapVersionBean map) {
    this.map = map;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(map, "map has not been set");

    log.info("Uninstalling map '{}'", map.getFolderName());
    Path mapPath = mapService.getPathForMap(map);
    FileUtils.deleteRecursively(mapPath);
    log.info("Map {} was uninstalled successfully", map.getFolderName());

    return null;
  }
}
