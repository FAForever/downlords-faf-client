package com.faforever.client.ui.effects;

import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Create a shadow effect for a given node and a specified depth level. depth levels are {0,1,2,3,4,5}
 */
public class DepthManager {

  private static DropShadow[] depth = new DropShadow[]{
      new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0), 0, 0, 0, 0),
      new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.26), 10, 0.12, -1, 2),
      new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.26), 15, 0.16, 0, 4),
      new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.26), 20, 0.19, 0, 6),
      new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.26), 25, 0.25, 0, 8),
      new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.26), 30, 0.30, 0, 10)};

  // TODO remove if unused
  /**
   * Add shadow effect to the node, however the shadow is not real (gets affected with node transformations)
   * <p>
   * use {@link #createMaterialNode(Node, int)} instead to generate a real shadow
   */
  public static void setDepth(Node control, int level) {
    level = level < 0 ? 0 : level;
    level = level > 5 ? 5 : level;
    control.setEffect(new DropShadow(BlurType.GAUSSIAN,
        depth[level].getColor(),
        depth[level].getRadius(),
        depth[level].getSpread(),
        depth[level].getOffsetX(),
        depth[level].getOffsetY()));
  }

  public static int getLevels() {
    return depth.length;
  }

  public static DropShadow getShadowAt(int level) {
    return depth[level];
  }


  // TODO remove if unused
  /**
   * Generate a new container node that prevent control transformation to be applied to the shadow
   * effect (which makes it looks as a real shadow)
   */
  public static Node createMaterialNode(Node control, int level) {
    Node container = new Pane(control) {
      @Override
      protected double computeMaxWidth(double height) {
        return computePrefWidth(height);
      }

      @Override
      protected double computeMaxHeight(double width) {
        return computePrefHeight(width);
      }

      @Override
      protected double computePrefWidth(double height) {
        return control.prefWidth(height);
      }

      @Override
      protected double computePrefHeight(double width) {
        return control.prefHeight(width);
      }
    };
    container.getStyleClass().add("depth-container");
    container.setPickOnBounds(false);
    level = level < 0 ? 0 : level;
    level = level > 5 ? 5 : level;
    container.setEffect(new DropShadow(BlurType.GAUSSIAN,
        depth[level].getColor(),
        depth[level].getRadius(),
        depth[level].getSpread(),
        depth[level].getOffsetX(),
        depth[level].getOffsetY()));
    return container;
  }

  public static void pop(Node control) {
    control.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.26), 5, 0.05, 0, 1));
  }

}
