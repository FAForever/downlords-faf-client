package com.faforever.client.map.management;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapBeanBuilder;
import com.faforever.client.map.MapService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.collections.FXCollections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class MapsManagementControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private MapService mapService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;

  private final MapBean officialMap = MapBeanBuilder.create().displayName("official map").folderName("SCMP_001").uid("official1")
      .version(null).get();
  private final MapBean customMap1 = MapBeanBuilder.create().displayName("custom map").folderName("palaneum.v0001").uid("custom1")
      .version(1).get();
  private final MapBean customMap2 = MapBeanBuilder.create().displayName("custom map").folderName("palaneum.v0002").uid("custom2")
      .version(2).get();

  private MapsManagementController instance;

  @Before
  public void setUp() throws Exception {
    when(mapService.isOfficialMap(officialMap)).thenReturn(true);
    when(mapService.isCustomMap(officialMap)).thenReturn(false);
    when(mapService.isCustomMap(customMap1)).thenReturn(true);
    when(mapService.isCustomMap(customMap2)).thenReturn(true);
    when(mapService.isOfficialMap(customMap1)).thenReturn(false);
    when(mapService.isOfficialMap(customMap2)).thenReturn(false);
    when(mapService.getInstalledMaps()).thenReturn(FXCollections.observableArrayList(officialMap, customMap1, customMap2));

    instance = new MapsManagementController(uiService, mapService, i18n);
    loadFxml("theme/vault/map/maps_management.fxml", param -> instance);
  }

  @Test
  public void testFilterMaps() {
    switchFilterTo(MapFilter.CUSTOM_MAPS);
    assertThat(instance.listView.getItems().size(), is(2));
    verifyItemsInList(customMap1, customMap2);

    switchFilterTo(MapFilter.OFFICIAL_MAPS);
    assertThat(instance.listView.getItems().size(), is(1));
    verifyItemsInList(officialMap);

    switchFilterTo(MapFilter.ALL_MAPS);
    assertThat(instance.listView.getItems().size(), is(3));
    verifyItemsInList(customMap1, customMap2, officialMap);
  }

  private void verifyItemsInList(MapBean... items) {
    Arrays.stream(items).forEach(item -> assertThat(instance.listView.getItems().contains(item), is(true)));
  }

  private void switchFilterTo(MapFilter filter) {
    runOnFxThreadAndWait(() -> instance.filterMapsChoiceBox.setValue(filter));
  }
}
