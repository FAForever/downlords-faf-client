package com.faforever.client.tutorial;

import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.application.Platform;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TutorialDetailControllerTest extends AbstractPlainJavaFxTest {
  private TutorialDetailController instance;
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private TutorialService tutorialService;

  @Before
  public void setUp() throws Exception {
    instance = new TutorialDetailController(i18n, mapService, webViewConfigurer, tutorialService);
    loadFxml("theme/tutorial_detail.fxml", clazz -> instance);
  }

  @Test
  public void loadExampleTutorial(){
    Tutorial tutorial = new Tutorial();
    tutorial.setTitle("title");
    tutorial.setDescription("description");
    tutorial.setLaunchable(true);
    MapBean mapVersion = new MapBean();
    tutorial.setMapVersion(mapVersion);
    Image image = new Image("http://examle.com");
    when(mapService.loadPreview(mapVersion, PreviewSize.LARGE)).thenReturn(image);
    Platform.runLater(() ->     instance.setTutorial(tutorial));
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).loadPreview(mapVersion, PreviewSize.LARGE);
    assertEquals(instance.mapImage.getImage(),image);
    assertEquals(instance.titleLabel.getText(),"title");
    assertTrue(instance.mapContainer.isVisible());
  }
}