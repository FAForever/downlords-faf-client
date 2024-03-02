package com.faforever.client.filter;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapSize;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import org.apache.commons.lang3.Range;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.function.BiFunction;

import static org.instancio.Select.field;
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

    assertTrue(filter.apply(Range.of(5, 100), Instancio.of(MapVersionBean.class)
                                                       .set(field(MapVersionBean::size), new MapSize(512, 512))
                                                       .create()));
    assertFalse(filter.apply(Range.of(30, 100), Instancio.of(MapVersionBean.class)
                                                         .set(field(MapVersionBean::size), new MapSize(1024, 1024))
                                                         .create()));
    assertTrue(filter.apply(AbstractRangeSliderFilterController.NO_CHANGE, Instancio.of(MapVersionBean.class)
                                                                                    .set(field(MapVersionBean::size),
                                                                                         new MapSize(256, 256))
                                                                                    .create()));
  }

  @Test
  public void testMapHeightFilter() {
    ArgumentCaptor<BiFunction<Range<Integer>, MapVersionBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(mapHeightFilter).registerListener(argumentCaptor.capture());

    BiFunction<Range<Integer>, MapVersionBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(Range.of(5, 100), Instancio.of(MapVersionBean.class)
                                                       .set(field(MapVersionBean::size), new MapSize(512, 512))
                                                       .create()));
    assertFalse(filter.apply(Range.of(30, 100), Instancio.of(MapVersionBean.class)
                                                         .set(field(MapVersionBean::size), new MapSize(1024, 1024))
                                                         .create()));
    assertTrue(filter.apply(AbstractRangeSliderFilterController.NO_CHANGE, Instancio.of(MapVersionBean.class)
                                                                                    .set(field(MapVersionBean::size),
                                                                                         new MapSize(256, 256))
                                                                                    .create()));
  }

  @Test
  public void testMapNumberOfPlayerFilter() {
    ArgumentCaptor<BiFunction<Range<Integer>, MapVersionBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(numberOfPlayersFilter).registerListener(argumentCaptor.capture());

    BiFunction<Range<Integer>, MapVersionBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(Range.of(1, 16),
                            Instancio.of(MapVersionBean.class).set(field(MapVersionBean::maxPlayers), 10).create()));
    assertFalse(filter.apply(Range.of(8, 100),
                             Instancio.of(MapVersionBean.class).set(field(MapVersionBean::maxPlayers), 4).create()));
    assertTrue(filter.apply(AbstractRangeSliderFilterController.NO_CHANGE,
                            Instancio.of(MapVersionBean.class).set(field(MapVersionBean::maxPlayers), 16).create()));
  }
}