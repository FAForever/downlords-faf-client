package com.faforever.client.fx;

import javafx.beans.binding.BooleanExpression;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.stage.Window;

// This class really only exists to make testing and insertion of the attached property easy without needing an argful
// constructor in the base Controller classes
public class AttachedUtil {

  public static BooleanExpression attachedProperty(Tab tab) {
    return BooleanExpression.booleanExpression(tab.tabPaneProperty()
                                                  .flatMap(Node::sceneProperty)
                                                  .flatMap(Scene::windowProperty)
                                                  .flatMap(Window::showingProperty)
                                                  .orElse(false));
  }

  public static BooleanExpression attachedProperty(Node node) {
    return BooleanExpression.booleanExpression(
        node.sceneProperty().flatMap(Scene::windowProperty).flatMap(Window::showingProperty).orElse(false));
  }

  public static BooleanExpression attachedProperty(MenuItem menuItem) {
    return BooleanExpression.booleanExpression(
        menuItem.parentMenuProperty().flatMap(Menu::showingProperty).orElse(false));
  }

}
