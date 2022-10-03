package com.faforever.client.filter;

import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapSize;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import org.apache.commons.lang3.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.function.BiFunction;

import static com.faforever.client.filter.FilterName.GAME_TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
public class MapFilterControllerTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;

  @InjectMocks
  private MapFilterController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/filter/filter.fxml", clazz -> instance, instance);
  }

  @Test
  public void testThrowExceptionWhenFilterNotExist() {
    assertThrows(IllegalArgumentException.class, () -> instance.setFollowingFilters(GAME_TYPE));
  }

  @Test
  public void testMapWidthFilter() {
    RangeSliderFilterController<MapVersionBean> controller = FilterTestUtil.mockFilter(RangeSliderFilterController.class, uiService);

    instance.setFollowingFilters(FilterName.MAP_WIDTH);

    ArgumentCaptor<BiFunction<Range<Integer>, MapVersionBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    BiFunction<Range<Integer>, MapVersionBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(Range.between(5, 100), MapVersionBeanBuilder.create()
        .size(MapSize.valueOf(512, 512))
        .get()));
    assertFalse(filter.apply(Range.between(30, 100), MapVersionBeanBuilder.create()
        .size(MapSize.valueOf(1024, 1024))
        .get()));
    assertTrue(filter.apply(AbstractRangeSliderFilterController.NO_CHANGE, MapVersionBeanBuilder.create()
        .size(MapSize.valueOf(256, 256))
        .get()));
  }

  @Test
  public void testMapHeightFilter() {
    RangeSliderFilterController<MapVersionBean> controller = FilterTestUtil.mockFilter(RangeSliderFilterController.class, uiService);

    instance.setFollowingFilters(FilterName.MAP_HEIGHT);

    ArgumentCaptor<BiFunction<Range<Integer>, MapVersionBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    BiFunction<Range<Integer>, MapVersionBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(Range.between(5, 100), MapVersionBeanBuilder.create()
        .size(MapSize.valueOf(512, 512))
        .get()));
    assertFalse(filter.apply(Range.between(30, 100), MapVersionBeanBuilder.create()
        .size(MapSize.valueOf(1024, 1024))
        .get()));
    assertTrue(filter.apply(AbstractRangeSliderFilterController.NO_CHANGE, MapVersionBeanBuilder.create()
        .size(MapSize.valueOf(256, 256))
        .get()));
  }

  @Test
  public void testMapNumberOfPlayerFilter() {
    RangeSliderFilterController<MapVersionBean> controller = FilterTestUtil.mockFilter(RangeSliderFilterController.class, uiService);

    instance.setFollowingFilters(FilterName.NUMBER_OF_PLAYERS);

    ArgumentCaptor<BiFunction<Range<Integer>, MapVersionBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    BiFunction<Range<Integer>, MapVersionBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(Range.between(1, 16), MapVersionBeanBuilder.create().maxPlayers(10).get()));
    assertFalse(filter.apply(Range.between(8, 100), MapVersionBeanBuilder.create().maxPlayers(4).get()));
    assertTrue(filter.apply(AbstractRangeSliderFilterController.NO_CHANGE, MapVersionBeanBuilder.create()
        .maxPlayers(16)
        .get()));
  }
}