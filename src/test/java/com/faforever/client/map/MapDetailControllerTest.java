package com.faforever.client.map;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.MapVersionReview;
import com.faforever.client.domain.server.PlayerInfo;
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
import org.instancio.Instancio;
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

import static org.instancio.Select.field;
import static org.instancio.Select.scope;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
  private ReviewsController<MapVersionReview> reviewsController;
  @Mock
  private ReviewController<MapVersionReview> reviewController;
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
  private PlayerInfo currentPlayer;
  private MapVersion testMap;
  private MapVersion ownedMap;
  private MapVersionReview review;

  private final BooleanProperty installed = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    currentPlayer = PlayerInfoBuilder.create().defaultValues().username("junit").id(100).get();
    testMap = Instancio.create(MapVersion.class);
    ownedMap = Instancio.of(MapVersion.class)
                        .set(field(Map::author).within(scope(Map.class)), currentPlayer)
                        .set(field(MapVersion::ranked), true)
                        .set(field(MapVersion::hidden), false)
                        .create();
    review = Instancio.of(MapVersionReview.class).set(field(MapVersionReview::player), currentPlayer).create();

    lenient().when(mapService.isInstalledBinding(Mockito.<ObservableValue<MapVersion>>any())).thenReturn(installed);
    lenient().when(imageViewHelper.createPlaceholderImageOnErrorObservable(any()))
             .thenAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0)));
    lenient().when(reviewService.getMapReviews(any())).thenReturn(Flux.empty());
    lenient().when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(currentPlayer));
    lenient().when(timeService.asDate(any(OffsetDateTime.class))).thenReturn("test date");
    lenient().when(playerService.getCurrentPlayer()).thenReturn(currentPlayer);
    lenient().when(mapService.downloadAndInstallMap(any(), any(DoubleProperty.class), any(StringProperty.class)))
             .thenReturn(Mono.empty());
    lenient().when(i18n.number(testMap.maxPlayers())).thenReturn(String.valueOf(testMap.maxPlayers()));
    lenient().when(i18n.get("map.id", testMap.id())).thenReturn(String.valueOf(testMap.id()));
    lenient().when(i18n.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(i18n.get(eq("mapPreview.size"), anyInt(), anyInt())).thenReturn("map size");
    lenient().when(mapService.isInstalled(testMap.folderName())).thenReturn(true);
    lenient().when(mapService.hasPlayedMap(eq(currentPlayer), eq(testMap))).thenReturn(Mono.just(true));
    lenient().when(mapService.getFileSize(any(MapVersion.class))).thenReturn(CompletableFuture.completedFuture(12));

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
    when(mapService.isInstalled(testMap.folderName())).thenReturn(false);

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
    runOnFxThreadAndWait(() -> instance.setMapVersion(ownedMap));

    assertNotEquals(instance.hideRow.getPrefHeight(), 0);
    assertTrue(instance.hideButton.isVisible());
    assertEquals("no", instance.isHiddenLabel.getText());
    assertEquals("yes", instance.isRankedLabel.getText());
    verify(reviewsController, times(2)).setCanWriteReview(false);
  }

  @Test
  public void testSetHiddenOwnedMap() {
    MapVersion ownedMap = Instancio.of(MapVersion.class)
                                   .set(field(Map::author).within(scope(Map.class)), currentPlayer)
                                   .set(field(MapVersion::ranked), false)
                                   .set(field(MapVersion::hidden), true)
                                   .create();

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
    assertEquals(testMap.map().displayName(), instance.nameLabel.getText());
    assertEquals(testMap.map().author().getUsername(), instance.authorLabel.getText());
    assertEquals(String.valueOf(testMap.maxPlayers()), instance.maxPlayersLabel.getText());
    assertEquals(String.valueOf(testMap.id()), instance.mapIdLabel.getText());
    assertEquals("map size", instance.dimensionsLabel.getText());
    assertEquals("test date", instance.dateLabel.getText());
    assertEquals(testMap.description(), instance.mapDescriptionLabel.getText());
    assertEquals(0.0, instance.hideRow.getPrefHeight(), 0);
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
    assertFalse(instance.hideButton.isVisible());
  }

  @Test
  public void testOnInstallButtonClicked() {
    when(mapService.downloadAndInstallMap(any(MapVersion.class), any(), any())).thenReturn(Mono.empty());

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).downloadAndInstallMap(any(MapVersion.class), any(), any());
  }

  @Test
  public void testOnInstallButtonClickedInstallingMapThrowsException() {
    when(mapService.downloadAndInstallMap(any(MapVersion.class), any(), any())).thenReturn(
        Mono.error(new FakeTestException()));

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    instance.onInstallButtonClicked();

    verify(mapService).downloadAndInstallMap(any(MapVersion.class), any(), any());
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyString(), anyString());
  }

  @Test
  public void testOnUninstallButtonClicked() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();
    when(mapService.uninstallMap(testMap)).thenReturn(Mono.empty());

    runOnFxThreadAndWait(() -> instance.onUninstallButtonClicked());

    verify(mapService).uninstallMap(testMap);
  }

  @Test
  public void testOnUninstallButtonClickedThrowsException() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    when(mapService.uninstallMap(testMap)).thenReturn(Mono.error(new FakeTestException()));

    runOnFxThreadAndWait(() -> instance.onUninstallButtonClicked());
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).uninstallMap(testMap);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyString(), anyString());
  }

  @Test
  public void testHideButtonClicked() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(ownedMap));
    when(mapService.hideMapVersion(ownedMap)).thenReturn(
        Mono.just(Instancio.of(MapVersion.class).set(field(MapVersion::hidden), true).create()));

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

    when(reviewService.deleteReview(review)).thenReturn(Mono.empty());

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).deleteReview(review);
  }

  @Test
  public void testOnDeleteReviewThrowsException() {
    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    when(reviewService.deleteReview(review)).thenReturn(Mono.error(new FakeTestException()));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.delete.error"));
  }

  @Test
  public void testOnSendReviewNew() {
    MapVersionReview review = Instancio.of(MapVersionReview.class)
                                       .ignore(field(MapVersionReview::id))
                                       .set(field(MapVersionReview::subject), testMap)
                                       .create();

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    when(reviewService.saveReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveReview(review);
  }

  @Test
  public void testOnSendReviewUpdate() {
    MapVersionReview review = Instancio.of(MapVersionReview.class)
                                       .set(field(MapVersionReview::subject), testMap)
                                       .create();

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();

    when(reviewService.saveReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveReview(review);
  }

  @Test
  public void testOnSendReviewThrowsException() {
    MapVersionReview review = Instancio.of(MapVersionReview.class)
                                       .set(field(MapVersionReview::subject), testMap)
                                       .create();

    runOnFxThreadAndWait(() -> instance.setMapVersion(testMap));
    WaitForAsyncUtils.waitForFxEvents();


    when(reviewService.saveReview(review)).thenReturn(Mono.error(new FakeTestException()));

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
