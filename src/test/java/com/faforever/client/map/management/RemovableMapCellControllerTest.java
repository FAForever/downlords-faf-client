package com.faforever.client.map.management;

import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapBeanBuilder;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemovableMapCellControllerTest extends UITest {

  @Mock
  private MapService mapService;
  @Mock
  private NotificationService notificationService;

  private final MapBean officialMap = MapBeanBuilder.create().displayName("official map").folderName("SCMP_001").uid("officialMap")
      .version(null).get();
  private final MapBean customMap = MapBeanBuilder.create().displayName("custom map").folderName("palaneum.v0001").uid("customMap")
      .version(1).get();

  private RemovableMapCellController instance;

  @BeforeEach
  public void setUp() throws Exception {
    when(mapService.isCustomMap(officialMap)).thenReturn(false);
    when(mapService.isCustomMap(customMap)).thenReturn(true);

    instance = new RemovableMapCellController(mapService, notificationService);
    loadFxml("theme/vault/map/removable_map_cell.fxml", param -> instance);
  }

  @Test
  public void testRemoveButtonInteractionForOfficialMap() {
    initializeCell(officialMap);
    assertThat(instance.removeButton.isDisabled(), is(true));
  }

  @Test
  public void testRemoveButtonInteractionForCustomMap() {
    initializeCell(customMap);
    assertThat(instance.removeButton.isDisabled(), is(false));
  }

  @Test
  public void testOnRemoveButtonClicked() {
    when(mapService.uninstallMap(customMap)).thenReturn(CompletableFuture.completedFuture(null));
    initializeCell(customMap);
    tapRemoveButton();
    verify(mapService).uninstallMap(customMap);
  }

    @Test
  public void testOnRemoveButtonClickedWhenThrowException() {
    when(mapService.uninstallMap(customMap))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("an error when uninstall map")));
    initializeCell(customMap);
    tapRemoveButton();
    verify(notificationService).addImmediateErrorNotification(any(RuntimeException.class), any());
  }

  private void initializeCell(MapBean map) {
    runOnFxThreadAndWait(() -> instance.updateItem(map, false));
  }

  private void tapRemoveButton() {
    runOnFxThreadAndWait(() -> instance.removeButton.getOnMouseClicked().handle(any()));
  }
}
