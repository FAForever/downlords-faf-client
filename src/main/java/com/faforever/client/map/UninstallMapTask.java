package com.faforever.client.map;

import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.task.CompletableTask;
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
public class UninstallMapTask extends CompletableTask<Void> {

  private final MapService mapService;

  private MapVersion map;

  @Autowired
  public UninstallMapTask(MapService mapService) {
    super(Priority.LOW);
    this.mapService = mapService;
  }

  public void setMap(MapVersion map) {
    this.map = map;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(map, "map has not been set");

    log.trace("Deleting map `{}`", map.folderName());
    Path mapPath = mapService.getPathForMap(map);
    FileSystemUtils.deleteRecursively(mapPath);
    log.trace("Map `{}` was deleted successfully", map.folderName());

    return null;
  }
}
