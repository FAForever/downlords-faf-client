package com.faforever.client.tournament.game;

import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.TutorialBeanBuilder;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.TutorialBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.test.UITest;
import com.faforever.client.tutorial.TutorialDetailController;
import com.faforever.client.tutorial.TutorialService;
import javafx.scene.image.Image;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class IsReadyForGameControllerTest extends UITest {
  @InjectMocks
  private IsReadyForGameController instance;
  @Mock
  private I18n i18n;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/tournaments/is_ready_for_game.fxml", clazz -> instance);
  }

  @Test
  public void testInitialization(){
    instance.setTimeout(1);
    instance.setDismissCallBack(() -> {});
    instance.setReadyCallback(() -> {});
  }

  @Test
  public void testTimeOut() {
    AtomicBoolean dismissCalled = new AtomicBoolean(false);
    instance.setDismissCallBack(() -> dismissCalled.set(true));
    instance.setTimeout(0);
    final var time = instance.queuePopTimeUpdater.getKeyFrames().get(1).getTime();
    WaitForAsyncUtils.sleep((long) time.toMillis()+1000, TimeUnit.MILLISECONDS);
    assertTrue(dismissCalled.get());
  }

  @Test
  public void testClickReady() {
    AtomicBoolean readyCallback = new AtomicBoolean(false);
    instance.setReadyCallback(() -> readyCallback.set(true));
    instance.isReadyButton.getOnAction().handle(null);
    instance.setTimeout(0);
    final var time = instance.queuePopTimeUpdater.getKeyFrames().get(1).getTime();
    WaitForAsyncUtils.sleep((long) time.toMillis()+1000, TimeUnit.MILLISECONDS);
    assertTrue(readyCallback.get());
  }
}