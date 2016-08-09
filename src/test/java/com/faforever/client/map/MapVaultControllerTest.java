package com.faforever.client.map;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import javafx.beans.Observable;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapVaultControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private MapService mapService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private MapDetailController mapDetailController;
  @Mock
  private EventBus eventBus;

  private MapVaultController instance;

  @Before
  public void setUp() throws Exception {
    instance = loadController("map_vault.fxml");
    instance.mapService = mapService;
    instance.applicationContext = applicationContext;
    instance.mapDetailController = mapDetailController;
    instance.eventBus = eventBus;

    when(mapDetailController.getRoot()).thenReturn(new Pane());

    instance.postConstruct();
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
  public void testSetUpIfNecessary() throws Exception {
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

    when(mapService.getMostDownloadedMaps(anyInt())).thenReturn(CompletableFuture.completedFuture(maps));
    when(mapService.getMostLikedMaps(anyInt())).thenReturn(CompletableFuture.completedFuture(maps));
    when(mapService.getNewestMaps(anyInt())).thenReturn(CompletableFuture.completedFuture(maps));
    when(mapService.getMostPlayedMaps(anyInt())).thenReturn(CompletableFuture.completedFuture(maps));

    MapTileController mapTileController = mock(MapTileController.class);
    doAnswer(invocation -> new Pane()).when(mapTileController).getRoot();

    when(applicationContext.getBean(MapTileController.class)).thenReturn(mapTileController);

    CountDownLatch latch = new CountDownLatch(3);
    waitUntilInitialized(instance.recommendedMapsPane, latch);
    waitUntilInitialized(instance.newestMapsPane, latch);
    waitUntilInitialized(instance.popularMapsPane, latch);

    instance.setUpIfNecessary();

    assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
  }

  private void waitUntilInitialized(Pane pane, CountDownLatch latch) {
    pane.getChildren().addListener((Observable observable) -> {
      if (pane.getChildren().size() == 2) {
        latch.countDown();
      }
    });
  }

  @Test
  public void testShowMapDetail() throws Exception {
    Pane pane = new Pane();
    pane.setVisible(false);
    when(mapDetailController.getRoot()).thenReturn(pane);

    MapBean mapBean = MapBeanBuilder.create().defaultValues().get();
    instance.onShowMapDetail(mapBean);

    verify(mapDetailController).setMap(mapBean);
    assertThat(pane.isVisible(), is(true));
  }
}
