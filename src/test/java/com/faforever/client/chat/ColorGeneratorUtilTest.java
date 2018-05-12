package com.faforever.client.chat;

import javafx.scene.paint.Color;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ColorGeneratorUtilTest {

  @Test
  public void testGenerateRandomHexColorRandomness() {

    long seed = 28823505;
    String shouldBe = "0x5547e5ff";
    Color[] result = new Color[10];

    for (int i = 0; i < 10; i++) {
      Color resultColor = ColorGeneratorUtil.generateRandomColor(seed);
      result[i] = resultColor;
    }
    for (Color resultColor : result) {
      assertEquals(shouldBe, resultColor.toString());
    }

    long seed2 = 96175180;
    String shouldBe2 = "0xbe4fe5ff";
    Color[] result2 = new Color[10];

    for (int i = 0; i < 10; i++) {
      Color resultColor = ColorGeneratorUtil.generateRandomColor(seed2);
      result2[i] = resultColor;
    }
    for (Color resultColor : result2) {
      assertEquals(shouldBe2, resultColor.toString());
    }

    long seed3 = 44183853;
    String shouldBe3 = "0x6bb7e5ff";
    Color[] result3 = new Color[10];

    for (int i = 0; i < 10; i++) {
      Color resultColor = ColorGeneratorUtil.generateRandomColor(seed3);
      result3[i] = resultColor;
    }
    for (Color resultColor : result3) {
      assertEquals(shouldBe3, resultColor.toString());
    }

  }
}
