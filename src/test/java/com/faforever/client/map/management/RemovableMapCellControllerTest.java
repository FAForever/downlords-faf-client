package com.faforever.client.map.management;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.PlatformTest;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemovableMapCellControllerTest extends PlatformTest {

  @Mock
  private MapService mapService;
  @Mock
  private NotificationService notificationService;

  private final MapVersionBean officialMap = Instancio.create(MapVersionBean.class);
  private final MapVersionBean customMap = Instancio.create(MapVersionBean.class);

  @InjectMocks
  private RemovableMapCellController instance;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(mapService.isCustomMap(customMap)).thenReturn(true);
    lenient().when(mapService.isOfficialMap(officialMap)).thenReturn(true);
    loadFxml("theme/vault/map/removable_map_cell.fxml", param -> instance);
  }

  @Test
  public void testRemoveButtonInteractionForOfficialMap() {
    instance.setMapVersion(officialMap);
    assertThat(instance.removeButton.isDisabled(), is(true));
  }

  @Test
  public void testRemoveButtonInteractionForCustomMap() {
    instance.setMapVersion(customMap);
    assertThat(instance.removeButton.isDisabled(), is(false));
  }

  @Test
  public void testOnRemoveButtonClicked() {
    when(mapService.uninstallMap(customMap)).thenReturn(Mono.empty());
    instance.setMapVersion(customMap);
    instance.removeButton.getOnMouseClicked().handle(null);
    verify(mapService).uninstallMap(customMap);
  }

  @Test
  public void testOnRemoveButtonClickedWhenThrowException() {
    when(mapService.uninstallMap(customMap)).thenReturn(
        Mono.error(new RuntimeException("an error when uninstall map")));
    instance.setMapVersion(customMap);
    instance.removeButton.getOnMouseClicked().handle(null);
    verify(notificationService).addImmediateErrorNotification(any(RuntimeException.class), any());
  }

}
