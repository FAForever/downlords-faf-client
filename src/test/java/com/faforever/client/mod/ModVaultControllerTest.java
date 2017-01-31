package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.beans.Observable;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
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

  @Before
  public void setUp() throws Exception {
    instance = new ModVaultController(modService, i18n, preferencesService, eventBus, uiService);

    doAnswer(invocation -> {
      ModCardController modCardController = mock(ModCardController.class);
      when(modCardController.getRoot()).then(invocation1 -> new Pane());
      return modCardController;
    }).when(uiService).loadFxml("theme/vault/mod/mod_card.fxml");

    loadFxml("theme/vault/mod/mod_vault.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.modVaultRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnDisplay() throws Exception {
    List<Mod> mods = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      mods.add(
          ModInfoBeanBuilder.create()
              .defaultValues()
              .name("Mod " + i)
              .uid(String.valueOf(i))
              .uiMod(i < 2)
              .get()
      );
    }

    when(modService.getMostDownloadedMods(anyInt())).thenReturn(CompletableFuture.completedFuture(mods));
    when(modService.getMostLikedUiMods(anyInt())).thenReturn(CompletableFuture.completedFuture(mods));
    when(modService.getNewestMods(anyInt())).thenReturn(CompletableFuture.completedFuture(mods));
    when(modService.getMostLikedMods(anyInt())).thenReturn(CompletableFuture.completedFuture(mods));

    CountDownLatch latch = new CountDownLatch(3);
    waitUntilInitialized(instance.recommendedUiModsPane, latch);
    waitUntilInitialized(instance.newestModsPane, latch);
    waitUntilInitialized(instance.popularModsPane, latch);

    instance.onDisplay();

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
  public void testShowModDetail() throws Exception {
    ModDetailController modDetailController = mock(ModDetailController.class);
    when(uiService.loadFxml("theme/vault/mod/mod_detail.fxml")).thenReturn(modDetailController);
    when(modDetailController.getRoot()).thenReturn(new Pane());

    Mod mod = ModInfoBeanBuilder.create().defaultValues().get();
    instance.onShowModDetail(mod);

    verify(modDetailController).setMod(mod);
    assertThat(modDetailController.getRoot().getParent(), is(notNullValue()));
  }

  @Test
  public void showMoreRecommendedUiMods() throws Exception {
    when(modService.getMostLikedUiMods(200)).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    instance.showMoreRecommendedUiMods();

    verify(modService).getMostLikedUiMods(200);
    assertThat(instance.showroomGroup.isVisible(), is(false));
    assertThat(instance.searchResultGroup.isVisible(), is(true));
  }

  @Test
  public void showMoreNewestMods() throws Exception {
    when(modService.getNewestMods(200)).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    instance.showMoreNewestMods();

    verify(modService).getNewestMods(200);
    assertThat(instance.showroomGroup.isVisible(), is(false));
    assertThat(instance.searchResultGroup.isVisible(), is(true));
  }

  @Test
  public void showMorePopularMods() throws Exception {
    when(modService.getMostPlayedMods(200)).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    instance.showMorePopularMods();

    verify(modService).getMostPlayedMods(200);
    assertThat(instance.showroomGroup.isVisible(), is(false));
    assertThat(instance.searchResultGroup.isVisible(), is(true));
  }

  @Test
  public void showMoreMostLikedMods() throws Exception {
    when(modService.getMostLikedMods(200)).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    instance.showMoreMostLikedMods();

    verify(modService).getMostLikedMods(200);
    assertThat(instance.showroomGroup.isVisible(), is(false));
    assertThat(instance.searchResultGroup.isVisible(), is(true));
  }
}
