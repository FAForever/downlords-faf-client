package com.faforever.client.chat;

import javafx.scene.paint.Color;

import java.util.Random;

public class ColorGeneratorUtil {

  //http://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/

  public static Color generatePrettyHexColor() {
    double goldenRatioConj = (1.0 + Math.sqrt(5.0)) / 2.0;
    float hue = new Random().nextFloat();
    hue += goldenRatioConj;
    hue = hue % 1;

    float saturation = randFloat(0.5f, 0.7f);

    return Color.hsb(hue * 360, saturation, 0.9f);
  }

  private static float randFloat(float min, float max) {
    if (Math.random() < 0.5) {
      return (float) (((1 - Math.random()) * (max - min)) + min);
    } else {
      return (float) ((Math.random() * (max - min)) + min);
    }
  }
}
