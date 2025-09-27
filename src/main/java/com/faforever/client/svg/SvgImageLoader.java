package com.faforever.client.svg;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.common.ImageLoaderImpl;
import de.codecentric.centerdevice.javafxsvg.BufferedImageTranscoder;
import de.codecentric.centerdevice.javafxsvg.FixedPixelDensityImageFrame;
import de.codecentric.centerdevice.javafxsvg.ScreenHelper;
import de.codecentric.centerdevice.javafxsvg.dimension.Dimension;
import de.codecentric.centerdevice.javafxsvg.dimension.DimensionProvider;
import javafx.stage.Screen;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_HEIGHT;
import static org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_WIDTH;

public class SvgImageLoader extends ImageLoaderImpl {

  private static final int BYTES_PER_PIXEL = 4; // RGBA

  private final InputStream input;
  private float maxPixelScale = 0;
  private final DimensionProvider dimensionProvider;


  protected SvgImageLoader(InputStream input, DimensionProvider dimensionProvider) {
    super(SvgDescriptor.getInstance());

    if (input == null) {
      throw new IllegalArgumentException("input == null!");
    }

    this.input = input;
    this.dimensionProvider = dimensionProvider;
  }

  @Override
  public ImageFrame load(int imageIndex, double width, double height, boolean preserveAspectRatio, boolean smooth,
                         float screenPixelScale, float imagePixelScale) throws IOException {
    if (0 != imageIndex) {
      return null;
    }

    Document document = createDocument();
    Dimension fallbackDimension = (width <= 0 || height <= 0) ? dimensionProvider.getDimension(document) : null;

    double imageWidth = width > 0 ? width : fallbackDimension.getWidth();
    double imageHeight = height > 0 ? height : fallbackDimension.getHeight();

    try {
      return createImageFrame(document, (float) imageWidth, (float) imageHeight, getPixelScale());
    } catch (TranscoderException ex) {
      throw new IOException(ex);
    }
  }

  private Document createDocument() throws IOException {
    return new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName()).createDocument(null, this.input);
  }

  public float getPixelScale() {
    if (maxPixelScale == 0) {
      maxPixelScale = calculateMaxRenderScale();
    }
    return maxPixelScale;
  }

  public float calculateMaxRenderScale() {
    float maxRenderScale = 0;
    ScreenHelper.ScreenAccessor accessor = ScreenHelper.getScreenAccessor();
    for (Screen screen : Screen.getScreens()) {
      maxRenderScale = Math.max(maxRenderScale, accessor.getRenderScale(screen));
    }
    return maxRenderScale;
  }

  private ImageFrame createImageFrame(Document document, float width, float height,
                                      float pixelScale) throws TranscoderException {
    BufferedImage bufferedImage = getTranscodedImage(document, width * pixelScale, height * pixelScale);
    ByteBuffer imageData = getImageData(bufferedImage);

    return new FixedPixelDensityImageFrame(ImageStorage.ImageType.RGBA, imageData, bufferedImage.getWidth(),
                                           bufferedImage.getHeight(), getStride(bufferedImage), null, pixelScale, null);
  }

  private BufferedImage getTranscodedImage(Document document, float width, float height) throws TranscoderException {
    BufferedImageTranscoder trans = new BufferedImageTranscoder(BufferedImage.TYPE_INT_ARGB);
    trans.addTranscodingHint(KEY_WIDTH, width);
    trans.addTranscodingHint(KEY_HEIGHT, height);
    trans.transcode(new TranscoderInput(document), null);

    return trans.getBufferedImage();
  }

  private int getStride(BufferedImage bufferedImage) {
    return bufferedImage.getWidth() * BYTES_PER_PIXEL;
  }

  private ByteBuffer getImageData(BufferedImage bufferedImage) {
    int[] rgb = bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null, 0,
                                     bufferedImage.getWidth());

    byte[] imageData = new byte[getStride(bufferedImage) * bufferedImage.getHeight()];

    copyColorToBytes(rgb, imageData);
    return ByteBuffer.wrap(imageData);
  }

  private void copyColorToBytes(int[] rgb, byte[] imageData) {
    if (rgb.length * BYTES_PER_PIXEL != imageData.length) {
      throw new ArrayIndexOutOfBoundsException();
    }

    ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);

    for (int i = 0; i < rgb.length; i++) {
      byte[] bytes = byteBuffer.putInt(rgb[i]).array();

      int dataOffset = BYTES_PER_PIXEL * i;
      imageData[dataOffset] = bytes[1];
      imageData[dataOffset + 1] = bytes[2];
      imageData[dataOffset + 2] = bytes[3];
      imageData[dataOffset + 3] = bytes[0];

      byteBuffer.clear();
    }
  }

  @Override
  public void dispose() {
    // Nothing to do
  }
}