package com.faforever.client.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class IdenticonUtil {

  private static final int PIXEL_COUNT = 8;
  private static final int IMAGE_SIZE = 128;

  private IdenticonUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static Image createIdenticon(Object object) {
    try {
      byte[] hash = MessageDigest.getInstance("MD5").digest(object.toString().getBytes());

      BufferedImage bufferedImage = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_4BYTE_ABGR);
      int pixelSize = IMAGE_SIZE / PIXEL_COUNT;

      Graphics graphics = bufferedImage.getGraphics();
      graphics.setColor(new Color(hash[0] & 255, hash[1] & 255, hash[2] & 255));

      int mirrorPixel = (int) Math.ceil(PIXEL_COUNT / 2f);
      for (int x = 0; x < PIXEL_COUNT; x++) {
        int pixelDeterminingIndex = x < mirrorPixel ? x : PIXEL_COUNT - 1 - x;

        for (int y = 0; y < PIXEL_COUNT; y++) {
          if ((hash[pixelDeterminingIndex] >> y & 1) == 1) {
            graphics.fillRect(x * pixelSize, y * pixelSize, pixelSize, pixelSize);
          }
        }
      }

      return SwingFXUtils.toFXImage(bufferedImage, new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight()));
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }
}
