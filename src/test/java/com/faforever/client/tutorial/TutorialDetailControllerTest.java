package com.faforever.client.tutorial;

import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.TutorialBean;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.test.PlatformTest;
import javafx.scene.image.Image;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TutorialDetailControllerTest extends PlatformTest {
  @InjectMocks
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
    loadFxml("theme/tutorial_detail.fxml", clazz -> instance);
  }

  @Test
  public void loadExampleTutorial(){
    MapVersionBean mapVersion = MapVersionBeanBuilder.create()
                                                     .defaultValues()
                                                     .map(Instancio.create(MapBean.class))
                                                     .get();
    TutorialBean tutorial = Instancio.of(TutorialBean.class)
                                     .set(field(TutorialBean::mapVersion), mapVersion)
                                     .set(field(TutorialBean::launchable), true)
                                     .create();

    Image image = new Image("http://example.com");
    when(mapService.loadPreview(mapVersion, PreviewSize.LARGE)).thenReturn(image);
    runOnFxThreadAndWait(() -> instance.setTutorial(tutorial));
    WaitForAsyncUtils.waitForFxEvents();
    verify(mapService).loadPreview(mapVersion, PreviewSize.LARGE);
    assertEquals(instance.mapImage.getImage(),image);
    assertEquals(instance.titleLabel.getText(), tutorial.title());
    assertTrue(instance.mapContainer.isVisible());
  }
}