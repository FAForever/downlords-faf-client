package com.faforever.client.mod;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.beans.Observable;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModVaultControllerTest extends AbstractPlainJavaFxTest {

  private ModVaultController instance;

  @Mock
  private ModService modService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private ModDetailController modDetailController;

  @Before
  public void setUp() throws Exception {
    instance = loadController("mod_vault.fxml");
    instance.modService = modService;
    instance.applicationContext = applicationContext;
    instance.modDetailController = modDetailController;

    when(modDetailController.getRoot()).thenReturn(new Pane());

    instance.postConstruct();
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.modVaultRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetUpIfNecessary() throws Exception {
    List<ModInfoBean> mods = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      mods.add(
          ModInfoBeanBuilder.create()
              .defaultValues()
              .uid(String.valueOf(i))
              .uidMod(i < 2)
              .get()
      );
    }

    ModTileController modTileController = mock(ModTileController.class);
    doAnswer(invocation -> new Pane()).when(modTileController).getRoot();

    when(applicationContext.getBean(ModTileController.class)).thenReturn(modTileController);
    CountDownLatch latch = new CountDownLatch(3);

    instance.recommendedUiModsPane.getChildren().addListener((Observable observable) -> {
      if (instance.recommendedUiModsPane.getChildren().size() == 2) {
        latch.countDown();
      }
    });
    instance.newestModsPane.getChildren().addListener((Observable observable) -> {
      if (instance.newestModsPane.getChildren().size() == 5) {
        latch.countDown();
      }
    });
    instance.popularModsPane.getChildren().addListener((Observable observable) -> {
      if (instance.popularModsPane.getChildren().size() == 5) {
        latch.countDown();
      }
    });

    instance.setUpIfNecessary();

    assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testShowModDetail() throws Exception {
    Node pane = new Pane();
    pane.setVisible(false);
    when(modDetailController.getRoot()).thenReturn(pane);

    ModInfoBean modInfoBean = ModInfoBeanBuilder.create().defaultValues().get();
    instance.onShowModDetail(modInfoBean);

    verify(modDetailController).setMod(modInfoBean);
    assertThat(pane.isVisible(), is(true));
  }
}
