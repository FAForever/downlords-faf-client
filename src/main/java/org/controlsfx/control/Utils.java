package org.controlsfx.control;

public class Utils {

  /**
   * Simple utility function which clamps the given value to be strictly between the min and max values.
   */
  public static double clamp(double min, double value, double max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }
}
