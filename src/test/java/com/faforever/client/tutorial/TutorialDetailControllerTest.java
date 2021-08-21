package com.faforever.client.tutorial;

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
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TutorialDetailControllerTest extends UITest {
  private TutorialDetailController instance;
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private TutorialService tutorialService;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new TutorialDetailController(i18n, mapService, webViewConfigurer, tutorialService);
    loadFxml("theme/tutorial_detail.fxml", clazz -> instance);
  }

  @Test
  public void loadExampleTutorial(){
    MapVersionBean mapVersion = MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get();
    TutorialBean tutorial = TutorialBeanBuilder.create().defaultValues().launchable(true).mapVersion(mapVersion).get();

    tutorial.setMapVersion(mapVersion);
    Image image = new Image("http://example.com");
    when(mapService.loadPreview(mapVersion, PreviewSize.LARGE)).thenReturn(image);
    JavaFxUtil.runLater(() -> instance.setTutorial(tutorial));
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).loadPreview(mapVersion, PreviewSize.LARGE);
    assertEquals(instance.mapImage.getImage(),image);
    assertEquals(instance.titleLabel.getText(),tutorial.getTitle());
    assertTrue(instance.mapContainer.isVisible());
  }
}