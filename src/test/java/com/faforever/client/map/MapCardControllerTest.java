package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.UITest;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import com.google.common.eventbus.EventBus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class MapCardControllerTest extends UITest {

  @Mock
  private MapService mapService;
  @Mock
  private MapGeneratorService mapGeneratorService;
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
  private ReviewsController reviewsController;
  @Mock
  private ReviewController reviewController;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;
  @Mock
  private EventBus eventBus;

  private MapCardController instance;
  private ObservableList<MapBean> installedMaps;
  private MapBean mapBean;

  @BeforeEach
  public void setUp() throws Exception {
    when(mapService.downloadAndInstallMap(any(), isNull(), isNull())).thenReturn(CompletableFuture.runAsync(() -> {
    }));
    when(mapService.uninstallMap(any())).thenReturn(CompletableFuture.runAsync(() -> {
    }));
    installedMaps = FXCollections.observableArrayList();
    when(mapService.getInstalledMaps()).thenReturn(installedMaps);
    instance = new MapCardController(mapService, mapGeneratorService, notificationService, i18n, reportingService);
    mapBean = new MapBean();
    mapBean.setFolderName("testMap");
    mapBean.setDisplayName("testMap");
    mapBean.setAuthor("axel12");
    mapBean.setRanked(true);
    mapBean.setHidden(false);
    mapBean.setId("23");
    mapBean.setSize(MapSize.valueOf(1, 1));
    mapBean.setDownloadUrl(new URL("http://google.com"));

    loadFxml("theme/vault/map/map_card.fxml", param -> {
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
  public void testSetMap() {
    instance.setMap(mapBean);

    assertThat(instance.nameLabel.getText(), is("testMap"));
    assertThat(instance.authorLabel.getText(), is("axel12"));
    assertThat(instance.thumbnailImageView.getImage(), is(notNullValue()));
  }

  @Test
  public void onInstallButtonClicked() {
    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }

  @Test
  public void onUninstallButtonClicked() {
    instance.onUninstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.uninstallButton.isVisible());
    assertTrue(instance.installButton.isVisible());
  }

  @Test
  public void onMapInstalled() {
    instance.setMap(mapBean);
    installedMaps.add(mapBean);
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }

  @Test
  public void onMapUninstalled() {
    instance.setMap(mapBean);
    installedMaps.add(mapBean);
    installedMaps.remove(mapBean);
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.uninstallButton.isVisible());
    assertTrue(instance.installButton.isVisible());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot().getParent(), is(nullValue()));
    assertThat((instance.getRoot()), is(instance.mapTileRoot));
  }
}

