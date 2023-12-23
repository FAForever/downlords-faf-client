package com.faforever.client.map;

import com.faforever.client.domain.MapVersionBean;
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
public class UninstallMapTask extends PrioritizedCompletableTask<Void> {

  private final MapService mapService;

  private MapVersionBean map;

  @Autowired
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

    log.trace("Deleting map `{}`", map.getFolderName());
    Path mapPath = mapService.getPathForMap(map);
    FileSystemUtils.deleteRecursively(mapPath);
    log.trace("Map `{}` was deleted successfully", map.getFolderName());

    return null;
  }
}
