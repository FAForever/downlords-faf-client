package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModVersion.ModType;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModCardControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  public ModService modService;
  @Mock
  private TimeService timeService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private I18n i18n;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  private ModCardController instance;
  private ModVersion modVersion;
  private ObservableList<ModVersion> installedModVersions;

  @Before
  public void setUp() throws Exception {
    instance = new ModCardController(modService, notificationService, timeService, i18n, reportingService);

    installedModVersions = FXCollections.observableArrayList();
    when(modService.getInstalledModVersions()).thenReturn(installedModVersions);
    when(i18n.get(ModType.UI.getI18nKey())).thenReturn(ModType.UI.name());

    modVersion = ModInfoBeanBuilder.create()
        .defaultValues()
        .name("ModVersion name")
        .modType(ModType.UI)
        .author("ModVersion author")
        .thumbnailUrl(getClass().getResource("/theme/images/default_achievement.png").toExternalForm())
        .get();

    when(modService.uninstallMod(any())).thenReturn(CompletableFuture.runAsync(() -> {
    }));
    when(modService.downloadAndInstallMod((ModVersion) any(), isNull(), isNull())).thenReturn(CompletableFuture.runAsync(() -> {
    }));

    loadFxml("theme/vault/mod/mod_card.fxml", clazz -> {
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
    instance.setModVersion(modVersion);

    assertThat(instance.nameLabel.getText(), is("ModVersion name"));
    assertThat(instance.authorLabel.getText(), is("ModVersion author"));
    assertThat(instance.thumbnailImageView.getImage(), is(notNullValue()));
    verify(modService).loadThumbnail(modVersion);
  }

  @Test
  public void testSetModNoThumbnail() {
    Image image = mock(Image.class);
    when(modService.loadThumbnail(modVersion)).thenReturn(image);

    instance.setModVersion(modVersion);

    assertThat(instance.thumbnailImageView.getImage(), notNullValue());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot().getParent(), is(nullValue()));
    assertThat((instance.getRoot()), is(instance.modTileRoot));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testShowModDetail() {
    Consumer<ModVersion> listener = mock(Consumer.class);
    instance.setOnOpenDetailListener(listener);
    instance.onShowModDetail();
    verify(listener).accept(any());
  }

  @Test
  public void testUiModLabel() {
    ModVersion modVersion = ModInfoBeanBuilder.create().defaultValues().modType(ModType.UI).get();
    instance.setModVersion(modVersion);
    assertThat(instance.typeLabel.getText(), equalTo(ModType.UI.name()));
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
    instance.setModVersion(modVersion);
    installedModVersions.add(modVersion);
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }

  @Test
  public void onMapUninstalled() {
    instance.setModVersion(modVersion);
    installedModVersions.add(modVersion);
    installedModVersions.remove(modVersion);
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.uninstallButton.isVisible());
    assertTrue(instance.installButton.isVisible());
  }
}
