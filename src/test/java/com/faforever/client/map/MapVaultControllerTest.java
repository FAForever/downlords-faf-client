package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.beans.Observable;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapVaultControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private MapService mapService;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;

  private MapVaultController instance;

  @Before
  public void setUp() throws Exception {
    instance = new MapVaultController(mapService, i18n, eventBus, preferencesService, uiService);

    doAnswer(invocation -> {
      MapDetailController controller = mock(MapDetailController.class);
      when(controller.getRoot()).thenReturn(new Pane());
      return controller;
    }).when(uiService).loadFxml("theme/vault/map/map_detail.fxml");

    doAnswer(invocation -> {
      MapCardController controller = mock(MapCardController.class);
      when(controller.getRoot()).thenReturn(new Pane());
      return controller;
    }).when(uiService).loadFxml("theme/vault/map/map_card.fxml");

    loadFxml("theme/vault/map/map_vault.fxml", clazz -> instance);
  }

  @Test
  public void testEventBusRegistered() throws Exception {
    verify(eventBus).register(instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.mapVaultRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnDisplay() throws Exception {
    List<MapBean> maps = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      maps.add(
          MapBeanBuilder.create()
              .defaultValues()
              .displayName("Map " + i)
              .uid(String.valueOf(i))
              .get()
      );
    }

    when(mapService.getMostLikedMaps(anyInt())).thenReturn(CompletableFuture.completedFuture(maps));
    when(mapService.getNewestMaps(anyInt())).thenReturn(CompletableFuture.completedFuture(maps));
    when(mapService.getMostPlayedMaps(anyInt())).thenReturn(CompletableFuture.completedFuture(maps));

    CountDownLatch latch = new CountDownLatch(3);
    waitUntilInitialized(instance.mostLikedPane, latch);
    waitUntilInitialized(instance.newestPane, latch);
    waitUntilInitialized(instance.mostPlayedPane, latch);

    instance.onDisplay();

    assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
  }

  private void waitUntilInitialized(Pane pane, CountDownLatch latch) {
    pane.getChildren().addListener((Observable observable) -> {
      if (pane.getChildren().size() == 2) {
        latch.countDown();
      }
    });
  }
}
