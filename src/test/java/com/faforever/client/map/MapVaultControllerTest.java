package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenMapVaultEvent;
import com.faforever.client.main.event.ShowLadderMapsEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.Button;

import javafx.beans.Observable;
import javafx.scene.layout.Pane;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.faforever.client.map.MapVaultController.*;

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
  @Mock
  private NotificationService notificationService;
  @Mock
  private SearchController searchController;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private PlayerService playerService;
  @Mock
  private ReportingService reportingService;

  private MapVaultController instance;


  private List<MapBean> createMockMaps(int numberOfMaps) {
    List<MapBean> maps = new ArrayList<>(numberOfMaps);
    for (int i = 0; i < numberOfMaps; i++) {
      maps.add(
          MapBeanBuilder.create()
              .defaultValues()
              .displayName("Map " + i)
              .uid(String.valueOf(i))
              .get()
      );
    }
    return maps;
  }
  
  private CompletableFuture<List<MapBean>> asFuture(List<MapBean> maps) {
    return CompletableFuture.completedFuture(maps);
  }
  
  private CompletableFuture<List<MapBean>> mocksAsFuture(int numberOfMaps) {
    return CompletableFuture.completedFuture(createMockMaps(numberOfMaps));
  }
  
  @Before
  public void setUp() throws Exception {
    when(preferencesService.getPreferences()).thenReturn(new Preferences());
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(new Player("junit")));
    
    instance = new MapVaultController(mapService, i18n, eventBus, preferencesService, uiService, notificationService,reportingService, playerService);

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

    loadFxml("theme/vault/map/map_vault.fxml", clazz -> {
      if (clazz == SearchController.class) {
        return searchController;
      }
      if (clazz == LogicalNodeController.class) {
        return logicalNodeController;
      }
      if (clazz == SpecificationController.class) {
        return specificationController;
      }
      return instance;
    });
    when(mapService.getHighestRatedMaps(anyInt(), anyInt())).thenReturn(mocksAsFuture(0));
    when(mapService.getNewestMaps(anyInt(), anyInt())).thenReturn(mocksAsFuture(0));
    when(mapService.getMostPlayedMaps(anyInt(), anyInt())).thenReturn(mocksAsFuture(0));
    when(mapService.getRecommendedMaps(anyInt(), anyInt())).thenReturn(mocksAsFuture(0));
    when(mapService.getLadderMaps(anyInt(), anyInt())).thenReturn(mocksAsFuture(0));
    when(mapService.getOwnedMaps(anyInt(), anyInt(), anyInt())).thenReturn(mocksAsFuture(0));
  }

  @Test
  public void testEventBusRegistered() {
    verify(eventBus).register(instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.mapVaultRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnDisplay() throws Exception {
    final int showroomCount = 5;
    List<MapBean> maps = createMockMaps(5);

    when(mapService.getRecommendedMaps(eq(TOP_ELEMENT_COUNT), eq(1))).thenReturn(asFuture(maps));
    when(mapService.getHighestRatedMaps(eq(TOP_ELEMENT_COUNT), eq(1))).thenReturn(asFuture(maps));
    when(mapService.getNewestMaps(eq(TOP_ELEMENT_COUNT), eq(1))).thenReturn(asFuture(maps));
    when(mapService.getMostPlayedMaps(eq(TOP_ELEMENT_COUNT), eq(1))).thenReturn(asFuture(maps));

    CountDownLatch latch = new CountDownLatch(3);
    waitUntilInitialized(instance.recommendedPane, latch, showroomCount);
    waitUntilInitialized(instance.mostLikedPane, latch, showroomCount);
    waitUntilInitialized(instance.newestPane, latch, showroomCount);
    waitUntilInitialized(instance.mostPlayedPane, latch, showroomCount);

    instance.display(new OpenMapVaultEvent());
    assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
  }

  private void waitUntilInitialized(Pane pane, CountDownLatch latch, int expectedNumberOfMaps) {
    pane.getChildren().addListener((Observable observable) -> {
      if (pane.getChildren().size() == expectedNumberOfMaps) {
        latch.countDown();
      }
    });
  }

  @Test
  public void testNotifyPropertyShowLadderInitialized() {
    when(mapService.getLadderMaps(eq(LOAD_PER_PAGE), eq(1))).thenReturn(mocksAsFuture(5));
    instance.display(new ShowLadderMapsEvent());

    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).getLadderMaps(LOAD_PER_PAGE, 1);
  }

  @Test
  public void testPagination() {
    final int lastPageCount = 20;
    List<MapBean> mapsPage1 = createMockMaps(LOAD_PER_PAGE);
    List<MapBean> mapsPage2 = mapsPage1.subList(0, LOAD_PER_PAGE);
    List<MapBean> mapsPage3 = mapsPage1.subList(0, lastPageCount);
    List<MapBean> mapsShowroom = mapsPage1.subList(0, TOP_ELEMENT_COUNT);

    // using recommended maps as example
    when(mapService.getRecommendedMaps(eq(TOP_ELEMENT_COUNT), eq(1))).thenReturn(asFuture(mapsShowroom));
    when(mapService.getRecommendedMaps(eq(LOAD_PER_PAGE), eq(1))).thenReturn(asFuture(mapsPage1));
    when(mapService.getRecommendedMaps(eq(LOAD_PER_PAGE), eq(2))).thenReturn(asFuture(mapsPage2));
    when(mapService.getRecommendedMaps(eq(LOAD_PER_PAGE), eq(3))).thenReturn(asFuture(mapsPage3));

    // showroom
    instance.display(new OpenMapVaultEvent());
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).getRecommendedMaps(TOP_ELEMENT_COUNT, 1);
    assertThat(instance.showroomGroup.isVisible(), is(true));
    assertThat(instance.searchResultGroup.isVisible(), is(false));
    assertThat(instance.recommendedPane.isVisible(), is(true));
    // size is one more, because a button is also in there
    assertThat(instance.recommendedPane.getChildren().size(), is(TOP_ELEMENT_COUNT + 1));

    // first page / search results
    instance.showMoreRecommendedMaps();
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).getRecommendedMaps(LOAD_PER_PAGE, 1);
    assertThat(instance.showroomGroup.isVisible(), is(false));
    assertThat(instance.searchResultGroup.isVisible(), is(true));
    assertThat(instance.searchResultPane.isVisible(), is(true));
    assertThat(instance.paginationHBox.isVisible(), is(true));
    assertButtonAvailability(instance.nextButton, true);
    assertButtonAvailability(instance.previousButton, false);
    assertThat(instance.searchResultPane.getChildren().size(), is(LOAD_PER_PAGE));

    // second page
    instance.nextButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).getRecommendedMaps(LOAD_PER_PAGE, 2);
    assertButtonAvailability(instance.nextButton, true);
    assertButtonAvailability(instance.previousButton, true);
    assertThat(instance.searchResultPane.getChildren().size(), is(LOAD_PER_PAGE));

    // third / last page
    instance.nextButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).getRecommendedMaps(LOAD_PER_PAGE, 3);
    assertButtonAvailability(instance.nextButton, false);
    assertButtonAvailability(instance.previousButton, true);
    assertThat(instance.searchResultPane.getChildren().size(), is(lastPageCount));

    // back button -> second page
    instance.previousButton.fire();
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService, times(2)).getRecommendedMaps(LOAD_PER_PAGE, 2);
  }
  
  private void assertButtonAvailability(Button button, boolean expectedIsAvailable) {
    if (expectedIsAvailable) {
      assertThat(button.isDisabled(), is(false));
      assertThat(button.isVisible(), is(true));
    } else {
      assertTrue(!button.isVisible() || button.isDisabled());
    }
  }

  @Test
  public void testPageRefresh() {
    // first we have certain number of maps
    when(mapService.getMostPlayedMaps(anyInt(), eq(1))).thenReturn(mocksAsFuture(2));
    instance.display(new OpenMapVaultEvent());
    WaitForAsyncUtils.waitForFxEvents();
    // size is one more, because a button is also in there
    assertThat(instance.mostPlayedPane.getChildren().size(), is(3));
    
    // more maps were added, refresh should update
    when(mapService.getMostPlayedMaps(anyInt(), eq(1))).thenReturn(mocksAsFuture(4));
    instance.onRefreshButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();
    // size is one more, because a button is also in there
    assertThat(instance.mostPlayedPane.getChildren().size(), is(5));
  }

  @Test
  public void testOwnedMapsHiddenWhenUserOwnsNoMaps() {
    when(mapService.getOwnedMaps(anyInt(), anyInt(), anyInt()))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    instance.display(new OpenMapVaultEvent());
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.ownedPane.isVisible()
        || instance.moreOwnedButton.isVisible()
        || instance.ownedMoreLabel.isVisible());
  }

  @Test
  public void testOwnedMapsShownWhenUserOwnsMaps() {
    when(mapService.getOwnedMaps(anyInt(), anyInt(), anyInt()))
        .thenReturn(CompletableFuture.completedFuture(createMockMaps(5)));
    instance.display(new OpenMapVaultEvent());
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.ownedPane.isVisible()
        && instance.moreOwnedButton.isVisible()
        && instance.ownedMoreLabel.isVisible());
  }
}
