package com.faforever.client.filter;

import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapFilterController extends AbstractFilterController<MapVersion> {

  public MapFilterController(UiService uiService, I18n i18n, FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(uiService, i18n, fxApplicationThreadExecutor);
  }

  @Override
  protected void build(FilterBuilder<MapVersion> filterBuilder) {
    filterBuilder.rangeSlider(i18n.get("game.filter.mapWidth"), 5, 100,
                              (range, mapVersion) -> range == AbstractRangeSliderFilterController.NO_CHANGE || range.contains(
                                  mapVersion.size().widthInKm()));

    filterBuilder.rangeSlider(i18n.get("game.filter.mapHeight"), 5, 100,
                              (range, mapVersion) -> range == AbstractRangeSliderFilterController.NO_CHANGE || range.contains(
                                  mapVersion.size().heightInKm()));

    filterBuilder.rangeSlider(i18n.get("game.filter.numberOfPlayers"), 1, 16,
                              (range, mapVersion) -> range == AbstractRangeSliderFilterController.NO_CHANGE || range.contains(
                                  mapVersion.maxPlayers()));
  }
}
