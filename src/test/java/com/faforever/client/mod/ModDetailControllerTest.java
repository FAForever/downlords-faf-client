package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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

  private ModDetailController instance;
  private ObservableList<ModInfoBean> installedMods;

  @Before
  public void setUp() throws Exception {
    instance = loadController("mod_detail.fxml");
    instance.modService = modService;
    instance.notificationService = notificationService;
    instance.reportingService = reportingService;
    instance.i18n = i18n;

    installedMods = FXCollections.observableArrayList();
    when(modService.getInstalledMods()).thenReturn(installedMods);
  }

  @Test
  public void testSetMod() throws Exception {
    ModInfoBean mod = ModInfoBeanBuilder.create()
        .defaultValues()
        .name("Mod name")
        .author("Mod author")
        .thumbnailUrl(getClass().getResource("/theme/images/tray_icon.png").toExternalForm())
        .get();

    when(modService.loadThumbnail(mod)).thenReturn(new Image("/theme/images/tray_icon.png"));
    instance.setMod(mod);

    assertThat(instance.nameLabel.getText(), is("Mod name"));
    assertThat(instance.authorLabel.getText(), is("Mod author"));
    assertThat(instance.thumbnailImageView.getImage(), is(notNullValue()));
    verify(modService).loadThumbnail(mod);
  }

  @Test
  public void testSetModNoThumbnailLoadsDefault() throws Exception {
    ModInfoBean mod = ModInfoBeanBuilder.create()
        .defaultValues()
        .thumbnailUrl(null)
        .get();
    Image image = mock(Image.class);
    when(modService.loadThumbnail(mod)).thenReturn(image);

    instance.setMod(mod);

    assertThat(instance.thumbnailImageView.getImage(), is(image));
  }

  @Test
  public void testOnInstallButtonClicked() throws Exception {
    when(modService.downloadAndInstallMod(any(ModInfoBean.class), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onInstallButtonClicked();

    verify(modService).downloadAndInstallMod(any(ModInfoBean.class), any(), any());
  }

  @Test
  public void testOnInstallButtonClickedInstallindModThrowsException() throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new Exception("test exception"));
    when(modService.downloadAndInstallMod(any(ModInfoBean.class), any(), any())).thenReturn(future);

    instance.setMod(ModInfoBeanBuilder.create().defaultValues().get());

    instance.onInstallButtonClicked();

    verify(modService).downloadAndInstallMod(any(ModInfoBean.class), any(), any());
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testOnUninstallButtonClicked() throws Exception {
    ModInfoBean mod = ModInfoBeanBuilder.create().defaultValues().get();
    instance.setMod(mod);
    when(modService.uninstallMod(mod)).thenReturn(CompletableFuture.completedFuture(null));

    instance.onUninstallButtonClicked();

    verify(modService).uninstallMod(mod);
  }

  @Test
  public void testOnUninstallButtonClickedThrowsException() throws Exception {
    ModInfoBean mod = ModInfoBeanBuilder.create().defaultValues().get();
    instance.setMod(mod);

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new Exception("test exception"));
    when(modService.uninstallMod(mod)).thenReturn(future);

    instance.onUninstallButtonClicked();

    verify(modService).uninstallMod(mod);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testOnCloseButtonClicked() throws Exception {
    assertThat(instance.modDetailRoot.isVisible(), is(true));
    instance.onCloseButtonClicked();
    assertThat(instance.modDetailRoot.isVisible(), is(false));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.modDetailRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testShowUninstallButtonWhenModIsInstalled() throws Exception {
    when(modService.isModInstalled("1")).thenReturn(true);
    instance.setMod(ModInfoBeanBuilder.create().defaultValues().uid("1").get());

    assertThat(instance.installButton.isVisible(), is(false));
    assertThat(instance.uninstallButton.isVisible(), is(true));
  }

  @Test
  public void testShowInstallButtonWhenModIsNotInstalled() throws Exception {
    when(modService.isModInstalled("1")).thenReturn(false);
    instance.setMod(ModInfoBeanBuilder.create().defaultValues().uid("1").get());

    assertThat(instance.installButton.isVisible(), is(true));
    assertThat(instance.uninstallButton.isVisible(), is(false));
  }

  @Test
  public void testChangeInstalledStateWhenModIsUninstalled() throws Exception {
    when(modService.isModInstalled("1")).thenReturn(true);
    ModInfoBean mod = ModInfoBeanBuilder.create().defaultValues().uid("1").get();
    instance.setMod(mod);
    installedMods.add(mod);

    assertThat(instance.installButton.isVisible(), is(false));
    assertThat(instance.uninstallButton.isVisible(), is(true));

    installedMods.remove(mod);

    assertThat(instance.installButton.isVisible(), is(true));
    assertThat(instance.uninstallButton.isVisible(), is(false));
  }

  @Test
  public void testChangeInstalledStateWhenModIsInstalled() throws Exception {
    when(modService.isModInstalled("1")).thenReturn(false);
    ModInfoBean mod = ModInfoBeanBuilder.create().defaultValues().uid("1").get();
    instance.setMod(mod);

    assertThat(instance.installButton.isVisible(), is(true));
    assertThat(instance.uninstallButton.isVisible(), is(false));

    installedMods.add(mod);

    assertThat(instance.installButton.isVisible(), is(false));
    assertThat(instance.uninstallButton.isVisible(), is(true));
  }
}
