package com.faforever.client.filter;

import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapSize;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import org.apache.commons.lang3.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class MapFilterControllerTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;

  @Mock
  private RangeSliderFilterController<MapVersionBean> mapWidthFilter;
  @Mock
  private RangeSliderFilterController<MapVersionBean> mapHeightFilter;
  @Mock
  private RangeSliderFilterController<MapVersionBean> numberOfPlayersFilter;

  @InjectMocks
  private MapFilterController instance;

  @BeforeEach
  public void setUp() throws Exception {
    // Order is important
    when(uiService.loadFxml(anyString(), eq(RangeSliderFilterController.class))).thenReturn(mapWidthFilter, mapHeightFilter, numberOfPlayersFilter);

    loadFxml("theme/filter/filter.fxml", clazz -> instance, instance);
  }

  @Test
  public void testMapWidthFilter() {
    ArgumentCaptor<BiFunction<Range<Integer>, MapVersionBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(mapWidthFilter).registerListener(argumentCaptor.capture());

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
    ArgumentCaptor<BiFunction<Range<Integer>, MapVersionBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(mapHeightFilter).registerListener(argumentCaptor.capture());

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
    ArgumentCaptor<BiFunction<Range<Integer>, MapVersionBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(numberOfPlayersFilter).registerListener(argumentCaptor.capture());

    BiFunction<Range<Integer>, MapVersionBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(Range.between(1, 16), MapVersionBeanBuilder.create().maxPlayers(10).get()));
    assertFalse(filter.apply(Range.between(8, 100), MapVersionBeanBuilder.create().maxPlayers(4).get()));
    assertTrue(filter.apply(AbstractRangeSliderFilterController.NO_CHANGE, MapVersionBeanBuilder.create()
        .maxPlayers(16)
        .get()));
  }
}