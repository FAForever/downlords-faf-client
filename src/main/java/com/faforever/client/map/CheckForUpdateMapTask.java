package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForUpdateMapTask extends CompletableTask<Optional<MapBean>> {

  private final MapService mapService;
  private final I18n i18n;

  private MapBean map;

  public CheckForUpdateMapTask(MapService mapService, I18n i18n) {
    super(Priority.LOW);
    this.mapService = mapService;
    this.i18n = i18n;
  }

  public CheckForUpdateMapTask setMap(MapBean map) {
    this.map = map;
    return this;
  }

  @Override
  protected Optional<MapBean> call() throws Exception {
    updateTitle(i18n.get("map.updater.search"));
    return mapService.getUpdatedMapIfExist(map).join();
  }
}
