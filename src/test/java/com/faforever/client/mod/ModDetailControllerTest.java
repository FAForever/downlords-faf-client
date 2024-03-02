package com.faforever.client.mod;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModDetailControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ModService modService;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private I18n i18n;
  @Mock
  private TimeService timeService;
  @Mock
  private ReviewService reviewService;
  @Mock
  private PlayerService playerService;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;
  @Mock
  private ReviewsController<ModVersionReviewBean> reviewsController;
  @Mock
  private ReviewController<ModVersionReviewBean> reviewController;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  @InjectMocks
  private ModDetailController instance;
  private PlayerBean currentPlayer;
  private ModVersionBean modVersion;

  private final SimpleBooleanProperty installed = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    currentPlayer = PlayerBeanBuilder.create().defaultValues().username("junit").get();
    modVersion = Instancio.create(ModVersionBean.class);

    lenient().when(imageViewHelper.createPlaceholderImageOnErrorObservable(any()))
             .thenAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0)));
    lenient().when(reviewService.getModReviews(any())).thenReturn(Flux.empty());
    lenient().when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());

    lenient().when(modService.isInstalledBinding(any())).thenReturn(installed);
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(currentPlayer));
    lenient().when(modService.getFileSize(any())).thenReturn(CompletableFuture.completedFuture(1024));
    lenient().when(i18n.get(eq("modVault.details.author"), anyString()))
             .thenAnswer(invocation -> invocation.getArgument(1));
    lenient().when(i18n.get("modVault.details.uploader", modVersion.mod().uploader().getUsername()))
             .thenReturn(modVersion.mod().uploader().getUsername());
    lenient().when(playerService.getCurrentPlayer()).thenReturn(currentPlayer);

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
    when(modService.loadThumbnail(modVersion)).thenReturn(new Image("/theme/images/default_achievement.png"));
    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(modVersion.mod().displayName(), instance.nameLabel.getText());
    assertEquals(modVersion.mod().author(), instance.authorLabel.getText());
    assertEquals(modVersion.mod().uploader().getUsername(), instance.uploaderLabel.getText());
    assertNotNull(instance.thumbnailImageView.getImage());
    verify(modService).getFileSize(modVersion);
    verify(modService).loadThumbnail(modVersion);
  }

  @Test
  public void testSetModWithNoUploader() {
    ModVersionBean modVersion = Instancio.of(ModVersionBean.class)
                                         .set(field(ModVersionBean::mod),
                                              Instancio.of(ModBean.class).set(field(ModBean::uploader), null).create())
                                         .create();

    when(modService.loadThumbnail(modVersion)).thenReturn(new Image("/theme/images/default_achievement.png"));
    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(modVersion.mod().displayName(), instance.nameLabel.getText());
    assertEquals(modVersion.mod().author(), instance.authorLabel.getText());
    assertNull(instance.uploaderLabel.getText());
    assertNotNull(instance.thumbnailImageView.getImage());
    verify(modService).loadThumbnail(modVersion);
  }

  @Test
  public void testSetModNoThumbnailLoadsDefault() {
    Image image = new Image(InputStream.nullInputStream());
    when(modService.loadThumbnail(modVersion)).thenReturn(image);

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(image, instance.thumbnailImageView.getImage());
  }

  @Test
  public void testOnInstallButtonClicked() {
    when(modService.downloadIfNecessary(any(ModVersionBean.class), any(), any())).thenReturn(Mono.empty());

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));
    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(modService).downloadIfNecessary(any(ModVersionBean.class), any(), any());
  }

  @Test
  public void testOnInstallButtonClickedInstallingModThrowsException() {
    when(modService.downloadIfNecessary(any(ModVersionBean.class), any(), any())).thenReturn(
        Mono.error(new FakeTestException()));

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    instance.onInstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(modService).downloadIfNecessary(any(ModVersionBean.class), any(), any());
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyString(), anyString());
  }

  @Test
  public void testOnUninstallButtonClicked() {
    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));
    when(modService.uninstallMod(modVersion)).thenReturn(CompletableFuture.completedFuture(null));

    instance.onUninstallButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(modService).uninstallMod(modVersion);
  }

  @Test
  public void testSetModNoSize() {
    when(modService.getFileSize(modVersion)).thenReturn(CompletableFuture.completedFuture(-1));
    when(i18n.get("modVault.install")).thenReturn("install");

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.installButton.isDisabled());
    assertEquals("install", instance.installButton.getText());
  }

  @Test
  public void testOnUninstallButtonClickedThrowsException() {
    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

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
    runOnFxThreadAndWait(() -> instance.onCloseButtonClicked());

    assertFalse(instance.modDetailRoot.isVisible());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.modDetailRoot, instance.getRoot());
  }

  @Test
  public void testSetInstalledMod() {
    installed.set(true);
    when(modService.isInstalled(modVersion.uid())).thenReturn(true);
    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    verify(reviewsController).setCanWriteReview(true);
    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());
  }

  @Test
  public void testSetUninstalledMod() {
    when(modService.isInstalled(modVersion.uid())).thenReturn(false);
    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    verify(reviewsController, times(2)).setCanWriteReview(false);
    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());
  }

  @Test
  public void testSetOwnedMod() {
    installed.set(true);
    ModBean modBean = Instancio.of(ModBean.class)
                               .set(field(ModBean::uploader), currentPlayer)
                               .set(field(ModBean::author), currentPlayer.getUsername())
                               .create();
    ModVersionBean modVersion = Instancio.of(ModVersionBean.class)
                                         .set(field(ModVersionBean::mod), Instancio.of(ModBean.class)
                                                                                   .set(field(ModBean::uploader),
                                                                                        currentPlayer)
                                                                                   .set(field(ModBean::author),
                                                                                        currentPlayer.getUsername())
                                                                                   .create())
                                         .create();
    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    verify(reviewsController, times(2)).setCanWriteReview(false);
    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());
  }

  @Test
  public void testChangeInstalledStateWhenModIsUninstalled() {
    when(modService.isInstalled(modVersion.uid())).thenReturn(true);
    installed.set(true);

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());

    installed.set(false);

    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());
  }

  @Test
  public void testChangeInstalledStateWhenModIsInstalled() {
    when(modService.isInstalled(modVersion.uid())).thenReturn(false);
    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    assertTrue(instance.installButton.isVisible());
    assertFalse(instance.uninstallButton.isVisible());

    installed.set(true);

    assertFalse(instance.installButton.isVisible());
    assertTrue(instance.uninstallButton.isVisible());
  }

  @Test
  public void testOnDeleteReview() {
    ModVersionReviewBean review = Instancio.of(ModVersionReviewBean.class)
                                           .set(field(ModVersionReviewBean::player), currentPlayer)
                                           .create();

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    when(reviewService.deleteReview(review)).thenReturn(Mono.empty());

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).deleteReview(review);
  }

  @Test
  public void testOnDeleteReviewThrowsException() {
    ModVersionReviewBean review = Instancio.of(ModVersionReviewBean.class)
                                           .set(field(ModVersionReviewBean::player), currentPlayer)
                                           .set(field(ModVersionReviewBean::player), currentPlayer)
                                           .create();

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    when(reviewService.deleteReview(review)).thenReturn(Mono.error(new FakeTestException()));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.delete.error"));
  }

  @Test
  public void testOnSendReviewNew() {
    ModVersionReviewBean review = Instancio.of(ModVersionReviewBean.class)
                                           .set(field(ModVersionReviewBean::player), currentPlayer)
                                           .set(field(ModVersionReviewBean::id), null)
                                           .set(field(ModVersionReviewBean::subject), modVersion)
                                           .create();

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    when(reviewService.saveReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveReview(review);
    assertEquals(currentPlayer, review.player());
  }

  @Test
  public void testOnSendReviewUpdate() {
    ModVersionReviewBean review = Instancio.of(ModVersionReviewBean.class)
                                           .set(field(ModVersionReviewBean::player), currentPlayer)
                                           .set(field(ModVersionReviewBean::id), 0)
                                           .set(field(ModVersionReviewBean::subject), modVersion)
                                           .create();

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    when(reviewService.saveReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveReview(review);
    assertEquals(currentPlayer, review.player());
  }

  @Test
  public void testOnSendReviewThrowsException() {
    ModVersionReviewBean review = Instancio.of(ModVersionReviewBean.class)
                                           .set(field(ModVersionReviewBean::player), currentPlayer)
                                           .set(field(ModVersionReviewBean::subject), modVersion)
                                           .create();

    runOnFxThreadAndWait(() -> instance.setModVersion(modVersion));

    when(reviewService.saveReview(review)).thenReturn(Mono.error(new FakeTestException()));

    runOnFxThreadAndWait(() -> instance.onSendReview(review));

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.save.error"));
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
