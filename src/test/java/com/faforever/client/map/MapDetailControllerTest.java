package com.faforever.client.map;

import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.MapVersionReviewBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.HostGameEvent;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapDetailControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private MapService mapService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private TimeService timeService;
  @Mock
  private PlayerService playerService;
  @Mock
  private ReviewService reviewService;
  @Mock
  private I18n i18n;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;
  @Mock
  private ReviewsController<MapVersionReviewBean> reviewsController;
  @Mock
  private ReviewController<MapVersionReviewBean> reviewController;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;
  @Mock
  private NavigationHandler navigationHandler;
  @Mock
  private ImageViewHelper imageViewHelper;

  @InjectMocks
  private MapDetailController instance;
  private PlayerBean currentPlayer;
  private MapVersionBean testMap;
  private MapVersionBean ownedMap;
  private MapVersionReviewBean review;

  private final BooleanProperty installed = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    currentPlayer = PlayerBeanBuilder.create().defaultValues().username("junit").id(100).get();
    testMap = MapVersionBeanBuilder.create()
        .defaultValues()
        .map(MapBeanBuilder.create().defaultValues().get())
        .createTime(OffsetDateTime.now())
        .get();
    ownedMap = MapVersionBeanBuilder.create()
        .defaultValues()
        .map(MapBeanBuilder.create().defaultValues().author(currentPlayer).get())
        .get();
    review = MapVersionReviewBeanBuilder.create().defaultValues().player(currentPlayer).get();

    when(mapService.isInstalledBinding(Mockito.<ObservableValue<MapVersionBean>>any())).thenReturn(installed);
    when(imageViewHelper.createPlaceholderImageOnErrorObservable(any())).thenAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0)));
    when(reviewService.getMapReviews(any())).thenReturn(Flux.empty());
    when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(currentPlayer));
    when(timeService.asDate(any(OffsetDateTime.class))).thenReturn("test date");
    when(playerService.getCurrentPlayer()).thenReturn(currentPlayer);
    when(mapService.downloadAndInstallMap(any(), any(DoubleProperty.class), any(StringProperty.class))).thenReturn(CompletableFuture.runAsync(() -> {
    }));
    when(i18n.number(testMap.getMaxPlayers())).thenReturn(String.valueOf(testMap.getMaxPlayers()));
    when(i18n.get("map.id", testMap.getId())).thenReturn(String.valueOf(testMap.getId()));
    when(i18n.get("yes")).thenReturn("yes");
    when(i18n.get("no")).thenReturn("no");
    when(i18n.get(eq("mapPreview.size"), anyInt(), anyInt())).thenReturn("map size");
    when(mapService.isInstalled(testMap.getFolderName())).thenReturn(true);
    when(mapService.hasPlayedMap(eq(currentPlayer), eq(testMap))).thenReturn(Mono.just(true));
    when(mapService.getFileSize(any(MapVersionBean.class))).thenReturn(CompletableFuture.completedFuture(12));

    loadFxml("theme/vault/map/map_detail.fxml", param -> {
      if (param == ReviewsController.class) {
        return reviewsController;
      }
      if (param == ReviewController.class) {
        return reviewController;
      }
      if (param == StarsController.class) {
        return starsController;
      }
      if (param == StarController.class) {
        return starController;
      }
      return instance;
    });
  }

  @Test
  public void onCreateButtonClickedMapNotInstalled() {
    when(mapService.isInstalled(testMap.getFolderName())).thenReturn(false);

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    instance.onCreateGameButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).downloadAndInstallMap(any(), any(DoubleProperty.class), any(StringProperty.class));
    verify(navigationHandler).navigateTo(any(HostGameEvent.class));
  }

  @Test
  public void onCreateButtonClickedMapInstalled() {

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    instance.onCreateGameButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();
    verify(navigationHandler).navigateTo(any(HostGameEvent.class));
  }

  @Test
  public void onInstallButtonClicked() {
    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).downloadAndInstallMap(any(), any(), any());
  }

  @Test
  public void testSetVisibleOwnedMap() {
    when(mapService.isInstalled(ownedMap.getFolderName())).thenReturn(true);

    runOnFxThreadAndWait(() -> instance.setMapVersion(ownedMap));

    assertNotEquals(instance.hideRow.getPrefHeight(), 0);
    assertTrue(instance.hideButton.isVisible());
    assertEquals("no", instance.isHiddenLabel.getText());
    assertEquals("yes", instance.isRankedLabel.getText());
    verify(reviewsController, times(2)).setCanWriteReview(false);
  }

  @Test
  public void testSetHiddenOwnedMap() {
    when(mapService.isInstalled(ownedMap.getFolderName())).thenReturn(true);

    ownedMap.setRanked(false);
    ownedMap.setHidden(true);

    runOnFxThreadAndWait(() -> instance.setMapVersion(ownedMap));
    WaitForAsyncUtils.waitForFxEvents();

    assertNotEquals(instance.hideRow.getPrefHeight(), 0);
    assertFalse(instance.hideButton.isVisible());
    assertEquals("yes", instance.isHiddenLabel.getText());
    assertEquals("no", instance.isRankedLabel.getText());
    verify(reviewsController, times(2)).setCanWriteReview(false);
  }

  @Test
  public void testSetMap() {
    installed.set(true);
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));

    verify(reviewsController).setCanWriteReview(true);
    assertEquals(testMap.getMap().getDisplayName(), instance.nameLabel.getText());
    assertEquals(testMap.getMap().getAuthor().getUsername(), instance.authorLabel.getText());
    assertEquals(String.valueOf(testMap.getMaxPlayers()), instance.maxPlayersLabel.getText());
    assertEquals(String.valueOf(testMap.getId()), instance.mapIdLabel.getText());
    assertEquals("map size", instance.dimensionsLabel.getText());
    assertEquals("test date", instance.dateLabel.getText());
    assertEquals(testMap.getDescription(), instance.mapDescriptionLabel.getText());
    assertEquals(0.0, instance.hideRow.getPrefHeight(), 0);
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
    assertFalse(instance.hideButton.isVisible());
  }

  @Test
  public void testOnInstallButtonClicked() {
    when(mapService.downloadAndInstallMap(any(MapVersionBean.class), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).downloadAndInstallMap(any(MapVersionBean.class), any(), any());
  }

  @Test
  public void testOnInstallButtonClickedInstallingMapThrowsException() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(mapService.downloadAndInstallMap(any(MapVersionBean.class), any(), any())).thenReturn(future);

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    instance.onInstallButtonClicked();

    verify(mapService).downloadAndInstallMap(any(MapVersionBean.class), any(), any());
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyString(), anyString());
  }

  @Test
  public void testOnUninstallButtonClicked() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();
    when(mapService.uninstallMap(testMap)).thenReturn(CompletableFuture.completedFuture(null));

    runOnFxThreadAndWait(() -> instance.onUninstallButtonClicked());

    verify(mapService).uninstallMap(testMap);
  }

  @Test
  public void testOnUninstallButtonClickedThrowsException() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(mapService.uninstallMap(testMap)).thenReturn(future);

    runOnFxThreadAndWait(() -> instance.onUninstallButtonClicked());
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).uninstallMap(testMap);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyString(), anyString());
  }

  @Test
  public void testHideButtonClicked() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(ownedMap));
    WaitForAsyncUtils.waitForFxEvents();
    when(mapService.hideMapVersion(ownedMap)).thenReturn(Mono.empty());

    runOnFxThreadAndWait(() -> instance.hideMap());

    assertFalse(instance.hideButton.isVisible());
    verify(mapService).hideMapVersion(ownedMap);
  }

  @Test
  public void testOnHideButtonClickedThrowsException() {

    runOnFxThreadAndWait(() -> instance.setMapVersion(ownedMap));
    WaitForAsyncUtils.waitForFxEvents();

    when(mapService.hideMapVersion(ownedMap)).thenReturn(Mono.error(new FakeTestException()));

    instance.hideMap();
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.hideButton.isVisible());
    verify(mapService).hideMapVersion(ownedMap);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testSetMapNotPlayed() {
    when(mapService.hasPlayedMap(currentPlayer, testMap)).thenReturn(Mono.just(false));

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));

    verify(reviewsController, times(2)).setCanWriteReview(false);
  }

  @Test
  public void testSetOfficialMap() {
    when(mapService.isOfficialMap(testMap)).thenReturn(true);

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));

    assertFalse(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());
  }

  @Test
  public void testSetMapNoSize() {
    when(mapService.getFileSize(testMap)).thenReturn(CompletableFuture.completedFuture(-1));
    when(i18n.get("mapVault.install")).thenReturn("install");

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));

    assertFalse(instance.installButton.isDisabled());
    assertEquals("install", instance.installButton.getText());
  }

  @Test
  public void testOnDeleteReview() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    when(reviewService.deleteMapVersionReview(review)).thenReturn(Mono.empty());

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).deleteMapVersionReview(review);
  }

  @Test
  public void testOnDeleteReviewThrowsException() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    when(reviewService.deleteMapVersionReview(review)).thenReturn(Mono.error(new FakeTestException()));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.delete.error"));
  }

  @Test
  public void testOnSendReviewNew() {
    review.setId(null);
    review.setMapVersion(testMap);

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    when(reviewService.saveMapVersionReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveMapVersionReview(review);
    assertEquals(currentPlayer, review.getPlayer());
  }

  @Test
  public void testOnSendReviewUpdate() {
    review.setMapVersion(testMap);
    review.setId(0);

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    when(reviewService.saveMapVersionReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveMapVersionReview(review);
    assertEquals(currentPlayer, review.getPlayer());
  }

  @Test
  public void testOnSendReviewThrowsException() {
    review.setMapVersion(testMap);

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();


    when(reviewService.saveMapVersionReview(review)).thenReturn(Mono.error(new FakeTestException()));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.save.error"));
  }

  @Test
  public void testChangeInstalledStateWhenModIsUninstalled() {
    installed.set(true);
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));

    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());

    installed.set(false);

    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());
  }

  @Test
  public void testChangeInstalledStateWhenMapIsInstalled() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));

    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());

    installed.set(true);

    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());
  }

  @Test
  public void testOnDimmerClicked() {
    instance.onDimmerClicked();

    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.getRoot().isVisible());
  }

  @Test
  public void testOnContentPaneClicked() {
    MouseEvent event = mock(MouseEvent.class);
    instance.onContentPaneClicked(event);

    WaitForAsyncUtils.waitForFxEvents();
    verify(event).consume();
  }
}
