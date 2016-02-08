package com.faforever.client.chat;

import javafx.scene.paint.Color;

import java.util.Random;

public class ColorGeneratorUtil {

  //http://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/

  public static Color generateRandomColor() {
    return generateRandomColor(0);
  }

  public static Color generateRandomColor(long seed) {
    double goldenRatioConj = (1.0 + Math.sqrt(5.0)) / 2.0;
    float saturation;
    float hue;
    if (seed == 0) {
      seed = new Random().nextLong();
    }
    saturation = randFloat(0.5f, 0.7f, seed);
    hue = new Random(seed).nextFloat();
    hue += goldenRatioConj;
    hue = hue % 1;

    return Color.hsb(hue * 360, saturation, 0.9f);
  }

  private static float randFloat(float min, float max, long seed) {
    Random generator = new Random(seed);
    if (generator.nextDouble() < 0.5) {
      return (float) (((1 - generator.nextDouble()) * (max - min)) + min);
    }

    return (float) ((generator.nextDouble() * (max - min)) + min);
  }
}
