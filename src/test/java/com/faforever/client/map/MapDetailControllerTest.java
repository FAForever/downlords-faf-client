package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.HostGameEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewBuilder;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapDetailControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private MapService mapService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private TimeService timeService;
  @Mock
  private PlayerService playerService;
  @Mock
  private ReviewService reviewService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private ReviewsController reviewsController;
  @Mock
  private ReviewController reviewController;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;
  @Mock
  private EventBus eventBus;

  private MapDetailController instance;
  private Player currentPlayer;
  private MapBean testMap;
  private MapBean ownedMap;
  private ObservableList<MapBean> installedMaps;
  private Review review;

  @Before
  public void setUp() throws Exception {
    currentPlayer = PlayerBuilder.create("junit").defaultValues().get();
    testMap = MapBeanBuilder.create().defaultValues().get();
    ownedMap = MapBeanBuilder.create().defaultValues().author(currentPlayer.getUsername()).get();
    review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();

    installedMaps = FXCollections.observableArrayList();
    installedMaps.add(testMap);

    when(timeService.asDate(any(LocalDateTime.class))).thenReturn("test date");
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(currentPlayer));
    when(mapService.downloadAndInstallMap(any(), any(DoubleProperty.class), any(StringProperty.class))).thenReturn(CompletableFuture.runAsync(() -> {
    }));
    when(i18n.number(testMap.getPlayers())).thenReturn(String.valueOf(testMap.getPlayers()));
    when(i18n.get("map.id", testMap.getId())).thenReturn(testMap.getId());
    when(i18n.get("yes")).thenReturn("yes");
    when(i18n.get("no")).thenReturn("no");
    when(i18n.get(eq("mapPreview.size"), anyInt(), anyInt())).thenReturn("map size");
    when(mapService.isInstalled(testMap.getFolderName())).thenReturn(true);
    when(mapService.hasPlayedMap(eq(currentPlayer.getId()), eq(testMap.getId()))).thenReturn(CompletableFuture.completedFuture(true));
    when(mapService.getFileSize(any(URL.class))).thenReturn(CompletableFuture.completedFuture(12));
    when(mapService.getInstalledMaps()).thenReturn(installedMaps);
    instance = new MapDetailController(mapService, notificationService, i18n, timeService, reportingService, playerService, reviewService, uiService, eventBus);

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

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    instance.onCreateGameButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).downloadAndInstallMap(any(), any(DoubleProperty.class), any(StringProperty.class));
    verify(eventBus).post(any(HostGameEvent.class));
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }

  @Test
  public void onCreateButtonClickedMapInstalled() {

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    instance.onCreateGameButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();
    verify(eventBus).post(any(HostGameEvent.class));
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }

  @Test
  public void onInstallButtonClicked() {
    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }

  @Test
  public void testSetRankedVisibleOwnedMap() {
    when(mapService.isInstalled(testMap.getFolderName())).thenReturn(true);

    instance.setMap(ownedMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertNotEquals(instance.hideRow.getPrefHeight(), 0);
    assertTrue(instance.unrankButton.isVisible());
    assertTrue(instance.hideButton.isVisible());
    assertEquals("no", instance.isHiddenLabel.getText());
    assertEquals("yes", instance.isRankedLabel.getText());
    verify(reviewsController, times(3)).setCanWriteReview(false);
  }

  @Test
  public void testSetUnRankedHiddenOwnedMap() {
    when(mapService.isInstalled(ownedMap.getFolderName())).thenReturn(true);

    ownedMap.setRanked(false);
    ownedMap.setHidden(true);

    instance.setMap(ownedMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertNotEquals(instance.hideRow.getPrefHeight(), 0);
    assertFalse(instance.unrankButton.isVisible());
    assertFalse(instance.hideButton.isVisible());
    assertEquals("yes", instance.isHiddenLabel.getText());
    assertEquals("no", instance.isRankedLabel.getText());
    verify(reviewsController, times(3)).setCanWriteReview(false);
  }

  @Test
  public void testSetMap() {
    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewsController).setCanWriteReview(true);
    assertEquals(testMap.getDisplayName(), instance.nameLabel.getText());
    assertEquals(testMap.getAuthor(), instance.authorLabel.getText());
    assertEquals(String.valueOf(testMap.getPlayers()), instance.maxPlayersLabel.getText());
    assertEquals(testMap.getId(), instance.mapIdLabel.getText());
    assertEquals("map size", instance.dimensionsLabel.getText());
    assertEquals("test date", instance.dateLabel.getText());
    assertEquals(testMap.getDescription(), instance.mapDescriptionLabel.getText());
    assertEquals(0.0, instance.hideRow.getPrefHeight(), 0);
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
    assertFalse(instance.unrankButton.isVisible());
    assertFalse(instance.hideButton.isVisible());
  }

  @Test
  public void testSetMapNoThumbnailLoadsDefault() {
    testMap.setLargeThumbnailUrl(null);

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService, never()).loadPreview(any(MapBean.class), any());
  }

  @Test
  public void testOnInstallButtonClicked() {
    when(mapService.downloadAndInstallMap(any(MapBean.class), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    instance.setMap(testMap);
    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).downloadAndInstallMap(any(MapBean.class), any(), any());
  }

  @Test
  public void testOnInstallButtonClickedInstallingMapThrowsException() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(mapService.downloadAndInstallMap(any(MapBean.class), any(), any())).thenReturn(future);

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    instance.onInstallButtonClicked();

    verify(mapService).downloadAndInstallMap(any(MapBean.class), any(), any());
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyString(), anyString());
  }

  @Test
  public void testOnUninstallButtonClicked() {
    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();
    when(mapService.uninstallMap(testMap)).thenReturn(CompletableFuture.completedFuture(null));

    instance.onUninstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).uninstallMap(testMap);
  }

  @Test
  public void testOnUninstallButtonClickedThrowsException() {
    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(mapService.uninstallMap(testMap)).thenReturn(future);

    instance.onUninstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).uninstallMap(testMap);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyString(), anyString());
  }

  @Test
  public void testOnUnRankButtonClicked() {
    when(mapService.unrankMapVersion(ownedMap)).thenReturn(CompletableFuture.completedFuture(null));

    instance.setMap(ownedMap);
    instance.unrankMap();
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).unrankMapVersion(any(MapBean.class));
    assertFalse(ownedMap.isRanked());
    assertFalse(instance.unrankButton.isVisible());
  }

  @Test
  public void testOnUnRankButtonClickedInstallingMapThrowsException() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(mapService.unrankMapVersion(any(MapBean.class))).thenReturn(future);

    instance.setMap(ownedMap);
    WaitForAsyncUtils.waitForFxEvents();

    instance.unrankMap();
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(ownedMap.isRanked());
    assertTrue(instance.unrankButton.isVisible());
    verify(mapService).unrankMapVersion(any(MapBean.class));
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testHideButtonClicked() {
    instance.setMap(ownedMap);
    WaitForAsyncUtils.waitForFxEvents();
    when(mapService.hideMapVersion(ownedMap)).thenReturn(CompletableFuture.completedFuture(null));

    instance.hideMap();
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(ownedMap.isHidden());
    assertFalse(instance.hideButton.isVisible());
    verify(mapService).hideMapVersion(ownedMap);
  }

  @Test
  public void testOnHideButtonClickedThrowsException() {

    instance.setMap(ownedMap);
    WaitForAsyncUtils.waitForFxEvents();

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(mapService.hideMapVersion(ownedMap)).thenReturn(future);

    instance.hideMap();
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(testMap.isHidden());
    assertTrue(instance.hideButton.isVisible());
    verify(mapService).hideMapVersion(ownedMap);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testSetMapNotPlayed() {
    when(mapService.hasPlayedMap(currentPlayer.getId(), testMap.getId())).thenReturn(CompletableFuture.completedFuture(false));

    instance.setMap(ownedMap);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewsController, times(3)).setCanWriteReview(false);
  }

  @Test
  public void testSetOfficialMap() {
    when(mapService.isOfficialMap(testMap.getFolderName())).thenReturn(true);

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());
  }

  @Test
  public void testSetMapNoSize() {
    when(mapService.getFileSize(testMap.getDownloadUrl())).thenReturn(CompletableFuture.completedFuture(-1));
    when(i18n.get("notAvailable")).thenReturn("notAvailable");

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.installButton.isDisabled());
    assertEquals("notAvailable", instance.installButton.getText());
  }

  @Test
  public void testOnDeleteReview() {
    testMap.getReviews().add(review);

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(testMap.getReviews().contains(review));

    when(reviewService.deleteMapVersionReview(review)).thenReturn(CompletableFuture.completedFuture(null));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).deleteMapVersionReview(review);
    assertFalse(testMap.getReviews().contains(review));
  }

  @Test
  public void testOnDeleteReviewThrowsException() {
    testMap.getReviews().add(review);

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(testMap.getReviews().contains(review));

    when(reviewService.deleteMapVersionReview(review)).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.delete.error"));
    assertTrue(testMap.getReviews().contains(review));
  }

  @Test
  public void testOnSendReviewNew() {
    review.setId(null);
    assertFalse(testMap.getReviews().contains(review));

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    when(reviewService.saveMapVersionReview(review, testMap.getId())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveMapVersionReview(review, testMap.getId());
    assertTrue(testMap.getReviews().contains(review));
    assertEquals(currentPlayer, review.getPlayer());
  }

  @Test
  public void testOnSendReviewUpdate() {
    testMap.getReviews().add(review);

    assertTrue(testMap.getReviews().contains(review));

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    when(reviewService.saveMapVersionReview(review, testMap.getId())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveMapVersionReview(review, testMap.getId());
    assertTrue(testMap.getReviews().contains(review));
    assertEquals(currentPlayer, review.getPlayer());
    assertEquals(1, testMap.getReviews().size());
  }

  @Test
  public void testOnSendReviewThrowsException() {
    testMap.getReviews().add(review);

    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(testMap.getReviews().contains(review));

    when(reviewService.saveMapVersionReview(review, testMap.getId())).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.save.error"));
    assertTrue(testMap.getReviews().contains(review));
  }

  @Test
  public void testChangeInstalledStateWhenModIsUninstalled() {
    when(mapService.isInstalled(testMap.getFolderName())).thenReturn(true);
    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());

    installedMaps.remove(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());
  }

  @Test
  public void testChangeInstalledStateWhenMapIsInstalled() {
    when(mapService.isInstalled(testMap.getFolderName())).thenReturn(false);
    installedMaps.remove(testMap);
    instance.setMap(testMap);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());

    installedMaps.add(testMap);
    WaitForAsyncUtils.waitForFxEvents();

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
