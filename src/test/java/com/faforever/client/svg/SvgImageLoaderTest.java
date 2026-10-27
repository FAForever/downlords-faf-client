package com.faforever.client.svg;

import com.faforever.client.test.PlatformTest;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SvgImageLoaderTest extends PlatformTest {

  private static final String SVG = """
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 42 46">
        <rect width="42" height="46" fill="#ff0000"/>
      </svg>
      """;

  @BeforeAll
  public static void installLoader() {
    SvgImageLoaderFactory.install();
  }

  @Test
  public void testLoadUsesIntrinsicSize() {
    Image image = new Image(new ByteArrayInputStream(SVG.getBytes(StandardCharsets.UTF_8)));

    assertFalse(image.isError(), () -> String.valueOf(image.getException()));
    assertEquals(42, image.getWidth());
    assertEquals(46, image.getHeight());
    assertEquals(0xFFFF0000, image.getPixelReader().getArgb(21, 23));
  }

  @Test
  public void testHugeDeclaredSizeIsCapped() {
    String hugeSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"12000\" height=\"12000\"/>";

    Image image = new Image(new ByteArrayInputStream(hugeSvg.getBytes(StandardCharsets.UTF_8)));

    assertFalse(image.isError(), () -> String.valueOf(image.getException()));
    assertTrue(image.getWidth() * image.getHeight() <= 2048 * 2048);
  }

  @Test
  public void testLoadWithRequestedSize() {
    Image image = new Image(new ByteArrayInputStream(SVG.getBytes(StandardCharsets.UTF_8)), 21, 23, true, true);

    assertFalse(image.isError(), () -> String.valueOf(image.getException()));
    assertEquals(21, image.getWidth());
    assertEquals(23, image.getHeight());
  }
}
