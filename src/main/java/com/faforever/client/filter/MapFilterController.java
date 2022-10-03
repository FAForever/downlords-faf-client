package com.faforever.client.filter;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.faforever.client.filter.FilterName.MAP_HEIGHT;
import static com.faforever.client.filter.FilterName.MAP_WIDTH;
import static com.faforever.client.filter.FilterName.NUMBER_OF_PLAYERS;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapFilterController extends AbstractFilterController<MapVersionBean> {

  protected MapFilterController(UiService uiService, I18n i18n) {
    super(uiService, i18n);
  }

  @Override
  protected void build(FilterBuilder<MapVersionBean> filterBuilder, List<FilterName> filterNames) {
    filterNames.forEach(filterName -> {

      switch (filterName) {

        case MAP_WIDTH -> filterBuilder.rangeSlider(MAP_WIDTH, i18n.get("game.filter.mapWidth"), 5, 100,
            (range, mapVersion) -> range == AbstractRangeSliderFilterController.NO_CHANGE || range.contains(mapVersion.getSize()
                .getWidthInKm()));

        case MAP_HEIGHT -> filterBuilder.rangeSlider(MAP_HEIGHT, i18n.get("game.filter.mapHeight"), 5, 100,
            (range, mapVersion) -> range == AbstractRangeSliderFilterController.NO_CHANGE || range.contains(mapVersion.getSize()
                .getHeightInKm()));

        case NUMBER_OF_PLAYERS ->
            filterBuilder.rangeSlider(NUMBER_OF_PLAYERS, i18n.get("game.filter.numberOfPlayers"), 1, 16,
                (range, mapVersion) -> range == AbstractRangeSliderFilterController.NO_CHANGE || range.contains(mapVersion.getMaxPlayers()));

        default -> throw new IllegalArgumentException("No implementation for the " + filterName.name() + " filter");
      }
    });
  }
}
