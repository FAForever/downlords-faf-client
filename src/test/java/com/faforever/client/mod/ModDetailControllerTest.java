package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
  private ReviewsController reviewsController;
  @Mock
  private ReviewController reviewController;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  private ModDetailController instance;
  private ObservableList<ModVersion> installedModVersions;

  @Before
  public void setUp() throws Exception {
    instance = new ModDetailController(modService, notificationService, i18n, reportingService, timeService, reviewService, playerService);

    installedModVersions = FXCollections.observableArrayList();
    when(modService.getInstalledModVersions()).thenReturn(installedModVersions);

    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(new Player("junit")));

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
    ModVersion modVersion = ModInfoBeanBuilder.create()
        .defaultValues()
        .name("ModVersion name")
        .author("ModVersion author")
        .thumbnailUrl(getClass().getResource("/theme/images/close.png").toExternalForm())
        .get();

    when(modService.loadThumbnail(modVersion)).thenReturn(new Image("/theme/images/close.png"));
    instance.setModVersion(modVersion);

    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.nameLabel.getText(), is("ModVersion name"));
    assertThat(instance.authorLabel.getText(), is("ModVersion author"));
    assertThat(instance.thumbnailImageView.getImage(), is(notNullValue()));
    verify(modService).loadThumbnail(modVersion);
  }

  @Test
  public void testSetModNoThumbnailLoadsDefault() {
    ModVersion modVersion = ModInfoBeanBuilder.create()
        .defaultValues()
        .thumbnailUrl(null)
        .get();
    Image image = mock(Image.class);
    when(modService.loadThumbnail(modVersion)).thenReturn(image);

    instance.setModVersion(modVersion);

    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.thumbnailImageView.getImage(), is(image));
  }

  @Test
  public void testOnInstallButtonClicked() {
    when(modService.downloadAndInstallMod(any(ModVersion.class), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    instance.setModVersion(ModInfoBeanBuilder.create().defaultValues().get());
    instance.onInstallButtonClicked();

    verify(modService).downloadAndInstallMod(any(ModVersion.class), any(), any());
  }

  @Test
  public void testOnInstallButtonClickedInstallindModThrowsException() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(modService.downloadAndInstallMod(any(ModVersion.class), any(), any())).thenReturn(future);

    instance.setModVersion(ModInfoBeanBuilder.create().defaultValues().get());

    instance.onInstallButtonClicked();

    verify(modService).downloadAndInstallMod(any(ModVersion.class), any(), any());
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testOnUninstallButtonClicked() {
    ModVersion modVersion = ModInfoBeanBuilder.create().defaultValues().get();
    instance.setModVersion(modVersion);
    when(modService.uninstallMod(modVersion)).thenReturn(CompletableFuture.completedFuture(null));

    instance.onUninstallButtonClicked();

    verify(modService).uninstallMod(modVersion);
  }

  @Test
  public void testOnUninstallButtonClickedThrowsException() {
    ModVersion modVersion = ModInfoBeanBuilder.create().defaultValues().get();
    instance.setModVersion(modVersion);

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(modService.uninstallMod(modVersion)).thenReturn(future);

    instance.onUninstallButtonClicked();

    verify(modService).uninstallMod(modVersion);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testOnCloseButtonClicked() {
    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().add(instance.getRoot()));
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.modDetailRoot.getParent(), is(notNullValue()));
    WaitForAsyncUtils.asyncFx(() -> instance.onCloseButtonClicked());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.modDetailRoot.isVisible(), is(false));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.modDetailRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testShowUninstallButtonWhenModIsInstalled() {
    when(modService.isModInstalled("1")).thenReturn(true);
    instance.setModVersion(ModInfoBeanBuilder.create().defaultValues().uid("1").get());

    assertThat(instance.installButton.isVisible(), is(false));
    assertThat(instance.uninstallButton.isVisible(), is(true));
  }

  @Test
  public void testShowInstallButtonWhenModIsNotInstalled() {
    when(modService.isModInstalled("1")).thenReturn(false);
    instance.setModVersion(ModInfoBeanBuilder.create().defaultValues().uid("1").get());

    assertThat(instance.installButton.isVisible(), is(true));
    assertThat(instance.uninstallButton.isVisible(), is(false));
  }

  @Test
  public void testChangeInstalledStateWhenModIsUninstalled() {
    when(modService.isModInstalled("1")).thenReturn(true);
    ModVersion modVersion = ModInfoBeanBuilder.create().defaultValues().uid("1").get();
    instance.setModVersion(modVersion);
    installedModVersions.add(modVersion);

    assertThat(instance.installButton.isVisible(), is(false));
    assertThat(instance.uninstallButton.isVisible(), is(true));

    installedModVersions.remove(modVersion);

    assertThat(instance.installButton.isVisible(), is(true));
    assertThat(instance.uninstallButton.isVisible(), is(false));
  }

  @Test
  public void testChangeInstalledStateWhenModIsInstalled() {
    when(modService.isModInstalled("1")).thenReturn(false);
    ModVersion modVersion = ModInfoBeanBuilder.create().defaultValues().uid("1").get();
    instance.setModVersion(modVersion);

    assertThat(instance.installButton.isVisible(), is(true));
    assertThat(instance.uninstallButton.isVisible(), is(false));

    installedModVersions.add(modVersion);

    assertThat(instance.installButton.isVisible(), is(false));
    assertThat(instance.uninstallButton.isVisible(), is(true));
  }
}
