package com.faforever.client.svg;

import com.sun.javafx.iio.ImageFormatDescription;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageLoaderFactory;
import com.sun.javafx.iio.ImageStorage;

import java.io.IOException;
import java.io.InputStream;

public class SvgImageLoaderFactory implements ImageLoaderFactory {
  private static final SvgImageLoaderFactory instance = new SvgImageLoaderFactory();

  public static void install() {
    ImageStorage.getInstance().addImageLoaderFactory(instance);
  }

  public static ImageLoaderFactory getInstance() {
    return instance;
  }

  @Override
  public ImageFormatDescription getFormatDescription() {
    return SvgDescriptor.getInstance();
  }

  @Override
  public ImageLoader createImageLoader(InputStream input) throws IOException {
    return new SvgImageLoader(input);
  }
}
