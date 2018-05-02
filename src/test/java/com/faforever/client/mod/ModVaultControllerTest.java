package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenModVaultEvent;
import com.faforever.client.mod.ModVersion.ModType;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController;
import com.google.common.eventbus.EventBus;
import javafx.beans.Observable;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModVaultControllerTest extends AbstractPlainJavaFxTest {

  private ModVaultController instance;

  @Mock
  private ModService modService;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private SearchController searchController;
  @Mock
  private SpecificationController specificationController;
  private ModDetailController modDetailController;

  @Before
  public void setUp() throws Exception {
    when(preferencesService.getPreferences()).thenReturn(new Preferences());
    instance = new ModVaultController(modService, i18n, eventBus, preferencesService, uiService, notificationService);

    doAnswer(invocation -> {
      ModCardController modCardController = mock(ModCardController.class);
      when(modCardController.getRoot()).then(invocation1 -> new Pane());
      return modCardController;
    }).when(uiService).loadFxml("theme/vault/mod/mod_card.fxml");

    doAnswer(invocation -> {
      modDetailController = mock(ModDetailController.class);
      when(modDetailController.getRoot()).then(invocation1 -> new Pane());
      return modDetailController;
    }).when(uiService).loadFxml("theme/vault/mod/mod_detail.fxml");

    ModVersion modVersion = ModInfoBeanBuilder.create().defaultValues().get();
    when(modService.getHighestRatedMods(100, 1)).thenReturn(CompletableFuture.completedFuture(Collections.singletonList(modVersion)));

    when(modService.getNewestMods(100, 1)).thenReturn(CompletableFuture.completedFuture(Collections.singletonList(modVersion)));

    loadFxml("theme/vault/mod/mod_vault.fxml", clazz -> {
      if (clazz == LogicalNodeController.class) {
        return logicalNodeController;
      }
      if (clazz == SearchController.class) {
        return searchController;
      }
      if (clazz == SpecificationController.class) {
        return specificationController;
      }
      return instance;
    });
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.modVaultRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnDisplay() throws Exception {
    List<ModVersion> modVersions = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      modVersions.add(
          ModInfoBeanBuilder.create()
              .defaultValues()
              .name("ModVersion " + i)
              .uid(String.valueOf(i))
              .modType(i < 2 ? ModType.UI : ModType.SIM)
              .get()
      );
    }

    when(modService.getNewestMods(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(modVersions));
    when(modService.getHighestRatedMods(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(modVersions));

    CountDownLatch latch = new CountDownLatch(2);
    waitUntilInitialized(instance.newestPane, latch);
    waitUntilInitialized(instance.highestRatedPane, latch);

    instance.onDisplay(new OpenModVaultEvent());

    assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
  }

  private void waitUntilInitialized(Pane pane, CountDownLatch latch) {
    pane.getChildren().addListener((Observable observable) -> {
      if (pane.getChildren().size() == 2) {
        latch.countDown();
      }
    });
  }

  @Test
  public void testShowModDetail() {
    ModVersion modVersion = ModInfoBeanBuilder.create().defaultValues().get();
    instance.onShowModDetail(modVersion);

    verify(modDetailController).setModVersion(modVersion);
    assertThat(modDetailController.getRoot().isVisible(), is(true));
  }

  @Test
  public void showMoreHighestRatedMods() {
    when(modService.getHighestRatedMods(100, 1)).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    instance.showMoreHighestRatedMods();

    WaitForAsyncUtils.waitForFxEvents();

    verify(modService).getHighestRatedMods(100, 1);
    assertThat(instance.showroomGroup.isVisible(), is(false));
    assertThat(instance.searchResultGroup.isVisible(), is(true));
  }

  @Test
  public void showMoreNewestMods() {
    when(modService.getNewestMods(100, 1)).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    instance.showMoreNewestMods();

    WaitForAsyncUtils.waitForFxEvents();

    verify(modService).getNewestMods(100, 1);
    assertFalse(instance.showroomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());
  }

  @Test
  public void showMoreMostLikedMods() {
    instance.showMoreHighestRatedMods();

    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.showroomGroup.isVisible());
    assertTrue(instance.searchResultGroup.isVisible());
  }
}
