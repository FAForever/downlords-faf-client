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

  private final MapBean officialMap = MapBeanBuilder.create().displayName("official map").folderName("SCMP_001")
      .version(null).get();
  private final MapBean customMap1 = MapBeanBuilder.create().displayName("custom map").folderName("palaneum.v0001")
      .version(1).get();
  private final MapBean customMap2 = MapBeanBuilder.create().displayName("custom map").folderName("palaneum.v0002")
      .version(2).get();

  private MapsManagementController instance;

  @Before
  public void setUp() throws Exception {
    when(mapService.isOfficialMap(officialMap)).thenReturn(true);
    when(mapService.isCustomMap(customMap1)).thenReturn(true);
    when(mapService.isCustomMap(customMap2)).thenReturn(true);
    when(mapService.getInstalledMaps()).thenReturn(FXCollections.observableArrayList(officialMap, customMap1, customMap2));

    instance = new MapsManagementController(uiService, mapService, i18n);
    loadFxml("theme/vault/map/maps_management.fxml", param -> instance);
  }

  @Test
  public void testFilterMaps() {
    switchFilterTo(MapFilter.CUSTOM_MAPS);
    assertThat(instance.listView.getItems().size(), is(2));
    verifyItemsInTable(customMap1, customMap2);

    switchFilterTo(MapFilter.OFFICIAL_MAPS);
    assertThat(instance.listView.getItems().size(), is(1));
    verifyItemsInTable(officialMap);

    switchFilterTo(MapFilter.ALL_MAPS);
    assertThat(instance.listView.getItems().size(), is(3));
    verifyItemsInTable(customMap1, customMap2, officialMap);
  }

//  @Test
//  public void testRemoveButtonInteraction() {
//    assertThat(instance.removeButton.isDisabled(), is(true));
//    selectMaps(customMap1);
//    assertThat(instance.removeButton.isDisabled(), is(false));
//
//    unselectMaps(customMap1);
//    assertThat(instance.removeButton.isDisabled(), is(true));
//
//    selectMaps(customMap1);
//    switchFilterTo(MapFilter.OFFICIAL_MAPS);
//    assertThat(instance.removeButton.isDisabled(), is(true));
//  }
//
//  @Test
//  public void testOnRemoveButtonClicked() {
//    when(mapService.uninstallMap(customMap1)).thenReturn(CompletableFuture.completedFuture(null));
//    selectMaps(customMap1);
//    assertThat(instance.getSelectedMaps().size(), is(1));
//    runOnFxThreadAndWait(() -> instance.onRemoveButtonClicked());
//    assertThat(instance.getSelectedMaps().isEmpty(), is(true));
//    verify(notificationService, never()).addImmediateErrorNotification(any(Exception.class), any());
//  }
//
//  @Test
//  public void testOnRemoveButtonClickedWhenThrowException() {
//    when(mapService.uninstallMap(customMap1))
//        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("an error when uninstall map")));
//    selectMaps(customMap1);
//    assertThat(instance.getSelectedMaps().size(), is(1));
//    runOnFxThreadAndWait(() -> instance.onRemoveButtonClicked());
//    assertThat(instance.getSelectedMaps().size(), is(1));
//    verify(notificationService).addImmediateErrorNotification(any(RuntimeException.class), any());
//  }

  private void verifyItemsInTable(MapBean... items) {
    Arrays.stream(items).forEach(item -> assertThat(instance.listView.getItems().contains(item), is(true)));
  }

  private void switchFilterTo(MapFilter filter) {
    runOnFxThreadAndWait(() -> instance.filterMapsChoiceBox.setValue(filter));
  }

//  private void selectMaps(MapBean... maps) {
//    verifyItemsInTable(maps);
//    assertThat(instance.getSelectedMaps().containsAll(Arrays.asList(maps)), is(false));
//    instance.getSelectedMaps().addAll(Arrays.asList(maps));
//  }
//
//  private void unselectMaps(MapBean... maps) {
//    verifyItemsInTable(maps);
//    assertThat(instance.getSelectedMaps().containsAll(Arrays.asList(maps)), is(true));
//    instance.getSelectedMaps().removeAll(Arrays.asList(maps));
//  }
}
