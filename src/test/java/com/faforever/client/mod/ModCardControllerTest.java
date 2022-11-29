package com.faforever.client.mod;

import com.faforever.client.builders.ModBeanBuilder;
import com.faforever.client.builders.ModVersionBeanBuilder;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModCardControllerTest extends UITest {

  @Mock
  public ModService modService;
  @Mock
  public UiService uiService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  @InjectMocks
  private ModCardController instance;
  private ModVersionBean modVersion;
  private ObservableList<ModVersionBean> installedModVersions;

  @BeforeEach
  public void setUp() throws Exception {
    installedModVersions = FXCollections.observableArrayList();
    when(modService.getInstalledModVersions()).thenReturn(installedModVersions);
    when(i18n.get(ModType.UI.getI18nKey())).thenReturn(ModType.UI.name());

    modVersion = ModVersionBeanBuilder.create().defaultValues().mod(ModBeanBuilder.create().defaultValues().get()).get();

    when(modService.uninstallMod(any())).thenReturn(CompletableFuture.runAsync(() -> {
    }));
    when(modService.downloadAndInstallMod((ModVersionBean) any(), isNull(), isNull())).thenReturn(CompletableFuture.runAsync(() -> {
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

    assertEquals(modVersion.getMod().getDisplayName(), instance.nameLabel.getText());
    assertEquals(modVersion.getMod().getAuthor(), instance.authorLabel.getText());
    assertNotNull(instance.thumbnailImageView.getImage());
    verify(modService).loadThumbnail(modVersion);
  }

  @Test
  public void testSetModNoThumbnail() {
    Image image = mock(Image.class);
    when(modService.loadThumbnail(modVersion)).thenReturn(image);

    instance.setModVersion(modVersion);

    assertNotNull(instance.thumbnailImageView.getImage());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertNull(instance.getRoot().getParent());
    assertEquals(instance.modTileRoot, instance.getRoot());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testShowModDetail() {
    Consumer<ModVersionBean> listener = mock(Consumer.class);
    instance.setOnOpenDetailListener(listener);
    instance.onShowModDetail();
    verify(listener).accept(any());
  }

  @Test
  public void testUiModLabel() {
    instance.setModVersion(modVersion);
    assertEquals(ModType.UI.name(), instance.typeLabel.getText());
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
  public void onModInstalled() {
    instance.setModVersion(modVersion);
    installedModVersions.add(modVersion);
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }

  @Test
  public void onModUninstalled() {
    instance.setModVersion(modVersion);
    installedModVersions.add(modVersion);
    installedModVersions.remove(modVersion);
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.uninstallButton.isVisible());
    assertTrue(instance.installButton.isVisible());
  }
}
