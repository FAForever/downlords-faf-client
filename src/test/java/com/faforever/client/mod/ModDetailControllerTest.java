package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModDetailControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private ReportingService reportingService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ModService modService;
  @Mock
  private I18n i18n;
  @Mock
  private TimeService timeService;
  @Mock
  private ReviewService reviewService;
  @Mock
  private PlayerService playerService;
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

  private ModDetailController instance;
  private ObservableList<ModVersion> installedModVersions;
  private Player currentPlayer;
  private ModVersion modVersion;

  @Before
  public void setUp() throws Exception {
    currentPlayer = PlayerBuilder.create("junit").defaultValues().get();
    modVersion = ModVersionBuilder.create().defaultValues().get();
    instance = new ModDetailController(modService, notificationService, i18n, reportingService, timeService, reviewService, playerService, uiService);

    installedModVersions = FXCollections.observableArrayList();
    when(modService.getInstalledModVersions()).thenReturn(installedModVersions);

    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(currentPlayer));

    loadFxml("theme/vault/mod/mod_detail.fxml", clazz -> {
      if (clazz == ReviewsController.class) {
        return reviewsController;
      }
      if (clazz == ReviewController.class) {
        return reviewController;
      }
      if (clazz == StarsController.class) {
        return starsController;
      }
      if (clazz == StarController.class) {
        return starController;
      }
      return instance;
    });
  }

  @Test
  public void testSetMod() {
    when(i18n.get("modVault.details.author", "ModVersion author")).thenReturn("ModVersion author");
    when(i18n.get("modVault.details.uploader", "ModVersion uploader")).thenReturn("ModVersion uploader");
    Review review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();
    modVersion.getReviews().add(review);

    when(modService.loadThumbnail(modVersion)).thenReturn(new Image("/theme/images/default_achievement.png"));
    instance.setModVersion(modVersion);

    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.nameLabel.getText(), "ModVersion name");
    assertEquals(instance.authorLabel.getText(), "ModVersion author");
    assertEquals(instance.uploaderLabel.getText(), "ModVersion uploader");
    assertNotNull(instance.thumbnailImageView.getImage());
    verify(modService).getModSize(modVersion);
    verify(modService).loadThumbnail(modVersion);
    verify(reviewsController).setOwnReview(Optional.of(review));
  }

  @Test
  public void testSetModWithNoUploader() {
    when(i18n.get("modVault.details.author", "ModVersion author")).thenReturn("ModVersion author");
    modVersion.getMod().setUploader(null);

    when(modService.loadThumbnail(modVersion)).thenReturn(new Image("/theme/images/default_achievement.png"));
    instance.setModVersion(modVersion);

    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.nameLabel.getText(), "ModVersion name");
    assertEquals(instance.authorLabel.getText(), "ModVersion author");
    assertNull(instance.uploaderLabel.getText());
    assertNotNull(instance.thumbnailImageView.getImage());
    verify(modService).loadThumbnail(modVersion);
  }

  @Test
  public void testSetModNoThumbnailLoadsDefault() {
    modVersion.setThumbnailUrl(null);
    Image image = mock(Image.class);
    when(modService.loadThumbnail(modVersion)).thenReturn(image);

    instance.setModVersion(modVersion);

    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.thumbnailImageView.getImage(), image);
  }

  @Test
  public void testOnInstallButtonClicked() {
    when(modService.downloadAndInstallMod(any(ModVersion.class), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    instance.setModVersion(modVersion);
    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(modService).downloadAndInstallMod(any(ModVersion.class), any(), any());
  }

  @Test
  public void testOnInstallButtonClickedInstallingModThrowsException() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(modService.downloadAndInstallMod(any(ModVersion.class), any(), any())).thenReturn(future);

    instance.setModVersion(modVersion);

    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(modService).downloadAndInstallMod(any(ModVersion.class), any(), any());
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyString(), anyString());
  }

  @Test
  public void testOnUninstallButtonClicked() {
    instance.setModVersion(modVersion);
    when(modService.uninstallMod(modVersion)).thenReturn(CompletableFuture.completedFuture(null));

    instance.onUninstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(modService).uninstallMod(modVersion);
  }

  @Test
  public void testOnUninstallButtonClickedThrowsException() {
    instance.setModVersion(modVersion);

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(modService.uninstallMod(modVersion)).thenReturn(future);

    instance.onUninstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(modService).uninstallMod(modVersion);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyString(), anyString());
  }

  @Test
  public void testOnCloseButtonClicked() {
    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().add(instance.getRoot()));
    WaitForAsyncUtils.waitForFxEvents();

    assertNotNull(instance.modDetailRoot.getParent());
    WaitForAsyncUtils.asyncFx(() -> instance.onCloseButtonClicked());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.modDetailRoot.isVisible());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.modDetailRoot);
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testSetInstalledMod() {
    when(modService.isModInstalled(modVersion.getUid())).thenReturn(true);
    instance.setModVersion(modVersion);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewsController).setCanWriteReview(true);
    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());
  }

  @Test
  public void testSetUninstalledMod() {
    when(modService.isModInstalled(modVersion.getUid())).thenReturn(false);
    instance.setModVersion(modVersion);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewsController, times(2)).setCanWriteReview(false);
    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());
  }

  @Test
  public void testSetOwnedMod() {
    when(modService.isModInstalled(modVersion.getUid())).thenReturn(true);
    instance.setModVersion(modVersion);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewsController, times(2)).setCanWriteReview(false);
    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());
  }

  @Test
  public void testChangeInstalledStateWhenModIsUninstalled() {
    when(modService.isModInstalled(modVersion.getUid())).thenReturn(true);
    instance.setModVersion(modVersion);
    installedModVersions.add(modVersion);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());

    installedModVersions.remove(modVersion);

    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());
  }

  @Test
  public void testChangeInstalledStateWhenModIsInstalled() {
    when(modService.isModInstalled(modVersion.getUid())).thenReturn(false);
    instance.setModVersion(modVersion);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());

    installedModVersions.add(modVersion);

    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());
  }

  @Test
  public void testOnDeleteReview() {
    Review review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();

    modVersion.getReviews().add(review);

    instance.setModVersion(modVersion);

    when(reviewService.deleteModVersionReview(review)).thenReturn(CompletableFuture.completedFuture(null));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).deleteModVersionReview(review);
    assertFalse(modVersion.getReviews().contains(review));
  }

  @Test
  public void testOnDeleteReviewThrowsException() {
    Review review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();

    modVersion.getReviews().add(review);

    instance.setModVersion(modVersion);

    when(reviewService.deleteModVersionReview(review)).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.delete.error"));
    assertTrue(modVersion.getReviews().contains(review));
  }

  @Test
  public void testOnSendReviewNew() {
    Review review = ReviewBuilder.create().defaultValues().id(null).get();

    instance.setModVersion(modVersion);

    when(reviewService.saveModVersionReview(review, modVersion.getId())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveModVersionReview(review, modVersion.getId());
    assertTrue(modVersion.getReviews().contains(review));
    assertEquals(review.getPlayer(), currentPlayer);
  }

  @Test
  public void testOnSendReviewUpdate() {
    Review review = ReviewBuilder.create().defaultValues().get();

    modVersion.getReviews().add(review);

    instance.setModVersion(modVersion);

    when(reviewService.saveModVersionReview(review, modVersion.getId())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveModVersionReview(review, modVersion.getId());
    assertTrue(modVersion.getReviews().contains(review));
    assertEquals(review.getPlayer(), currentPlayer);
    assertEquals(modVersion.getReviews().size(), 1);
  }

  @Test
  public void testOnSendReviewThrowsException() {
    Review review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();

    modVersion.getReviews().add(review);

    instance.setModVersion(modVersion);

    when(reviewService.saveModVersionReview(review, modVersion.getId())).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.save.error"));
    assertTrue(modVersion.getReviews().contains(review));
  }

  @Test
  public void testOnDimmerClicked() {
    instance.onDimmerClicked();

    assertFalse(instance.getRoot().isVisible());
  }

  @Test
  public void testOnContentPaneClicked() {
    MouseEvent event = mock(MouseEvent.class);
    instance.onContentPaneClicked(event);

    verify(event).consume();
  }
}
