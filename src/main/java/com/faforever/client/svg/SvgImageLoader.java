package com.faforever.client.svg;

import com.faforever.client.fx.JavaFxUtil;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.common.ImageLoaderImpl;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SvgImageLoader extends ImageLoaderImpl {

  private static final int BYTES_PER_PIXEL = 4; // RGBA
  /** Upper bound of rendered pixels, so a tiny SVG declaring a huge size can't exhaust the heap. */
  private static final double MAX_PIXELS = 2048 * 2048;

  private final InputStream input;
  private float maxPixelScale = 0;


  protected SvgImageLoader(InputStream input) {
    super(SvgDescriptor.getInstance());

    if (input == null) {
      throw new IllegalArgumentException("input == null!");
    }

    this.input = input;
  }

  @Override
  public ImageFrame load(int imageIndex, double width, double height, boolean preserveAspectRatio, boolean smooth,
                         float screenPixelScale, float imagePixelScale) throws IOException {
    if (0 != imageIndex) {
      return null;
    }

    SVGDocument document = new SVGLoader().load(input, null, LoaderContext.createDefault());
    if (document == null) {
      throw new IOException("Could not parse SVG");
    }

    FloatSize intrinsicSize = document.size();
    double imageWidth = width > 0 ? width : intrinsicSize.width;
    double imageHeight = height > 0 ? height : intrinsicSize.height;
    if (!Double.isFinite(imageWidth) || !Double.isFinite(imageHeight) || imageWidth <= 0 || imageHeight <= 0) {
      throw new IOException("Invalid SVG dimensions: " + imageWidth + "x" + imageHeight);
    }

    float pixelScale = getPixelScale();
    double pixels = imageWidth * imageHeight * pixelScale * pixelScale;
    if (pixels > MAX_PIXELS) {
      double downscale = Math.sqrt(MAX_PIXELS / pixels);
      imageWidth *= downscale;
      imageHeight *= downscale;
    }

    return createImageFrame(document, (float) imageWidth, (float) imageHeight, pixelScale);
  }

  public float getPixelScale() {
    if (maxPixelScale == 0) {
      maxPixelScale = calculateMaxRenderScale();
    }
    return maxPixelScale;
  }

  public float calculateMaxRenderScale() {
    return (float) JavaFxUtil.getMaxOutputScale();
  }

  private ImageFrame createImageFrame(SVGDocument document, float width, float height, float pixelScale) {
    BufferedImage bufferedImage = renderImage(document, width, height, pixelScale);
    ByteBuffer imageData = getImageData(bufferedImage);

    return new ImageFrame(ImageStorage.ImageType.RGBA, imageData, bufferedImage.getWidth(), bufferedImage.getHeight(),
                          getStride(bufferedImage), pixelScale, null);
  }

  private BufferedImage renderImage(SVGDocument document, float width, float height, float pixelScale) {
    BufferedImage bufferedImage = new BufferedImage(Math.max(1, Math.round(width * pixelScale)),
                                                    Math.max(1, Math.round(height * pixelScale)),
                                                    BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = bufferedImage.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      graphics.scale(pixelScale, pixelScale);
      document.render(null, graphics, new ViewBox(width, height));
    } finally {
      graphics.dispose();
    }
    return bufferedImage;
  }

  private int getStride(BufferedImage bufferedImage) {
    return bufferedImage.getWidth() * BYTES_PER_PIXEL;
  }

  private ByteBuffer getImageData(BufferedImage bufferedImage) {
    int[] argb = bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null, 0,
                                      bufferedImage.getWidth());

    byte[] imageData = new byte[getStride(bufferedImage) * bufferedImage.getHeight()];
    for (int i = 0; i < argb.length; i++) {
      int pixel = argb[i];
      int dataOffset = BYTES_PER_PIXEL * i;
      imageData[dataOffset] = (byte) (pixel >> 16);
      imageData[dataOffset + 1] = (byte) (pixel >> 8);
      imageData[dataOffset + 2] = (byte) pixel;
      imageData[dataOffset + 3] = (byte) (pixel >> 24);
    }
    return ByteBuffer.wrap(imageData);
  }

  @Override
  public void dispose() {
    // Nothing to do
  }
}
