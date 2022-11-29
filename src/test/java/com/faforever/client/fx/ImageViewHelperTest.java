package com.faforever.client.fx;

import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ImageViewHelperTest extends UITest {

  @Mock
  private UiService uiService;

  @InjectMocks
  private ImageViewHelper instance;

  private ImageView imageView;
  private Image defaultImage;

  @BeforeEach
  public void setUp() {
    imageView = new ImageView();
    defaultImage = new Image(new ClassPathResource("/images/hydro.png").getPath());
  }

  @Test
  public void testSetPlaceholderImage() {
    Mockito.when(uiService.getThemeImage(Mockito.anyString())).thenReturn(defaultImage);

    instance.setDefaultPlaceholderImage(imageView);
    assertEquals(defaultImage, imageView.getImage());

    Image brokenImage = new Image("http://localhost/broken_image.png", true);
    runOnFxThreadAndWait(() -> imageView.setImage(brokenImage));
    assertEquals(defaultImage, imageView.getImage());

    Image regularImage = new Image(new ClassPathResource("/images/mass.png").getPath(), true);
    runOnFxThreadAndWait(() -> imageView.setImage(regularImage));
    assertEquals(regularImage, imageView.getImage());

    Image brokenImage1 = new Image("http://localhost/broken_image1.png", true);
    runOnFxThreadAndWait(() -> imageView.setImage(brokenImage1));
    assertEquals(defaultImage, imageView.getImage());

    runOnFxThreadAndWait(() -> imageView.setImage(null));
    assertNull(imageView.getImage());
  }

  @Test
  public void testSetPlaceholderImageWhenOnlyOnError() {
    Mockito.when(uiService.getThemeImage(Mockito.anyString())).thenReturn(defaultImage);

    instance.setDefaultPlaceholderImage(imageView, true);
    assertNull(imageView.getImage());

    Image brokenImage = new Image("http://localhost/broken_image.png", true);
    runOnFxThreadAndWait(() -> imageView.setImage(brokenImage));
    assertEquals(defaultImage, imageView.getImage());

    Image regularImage = new Image(new ClassPathResource("/images/mass.png").getPath(), true);
    runOnFxThreadAndWait(() -> imageView.setImage(regularImage));
    assertEquals(regularImage, imageView.getImage());

    Image brokenImage1 = new Image("http://localhost/broken_image1.png", true);
    runOnFxThreadAndWait(() -> imageView.setImage(brokenImage1));
    assertEquals(defaultImage, imageView.getImage());

    runOnFxThreadAndWait(() -> imageView.setImage(null));
    assertNull(imageView.getImage());
  }
}