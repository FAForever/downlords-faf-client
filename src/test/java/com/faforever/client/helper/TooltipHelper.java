package com.faforever.client.helper;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class TooltipHelper {

  public Tooltip getTooltip(Node node) {
    return (Tooltip) node.getProperties()
        .values()
        .stream()
        .filter(object -> object.getClass().isAssignableFrom(Tooltip.class))
        .findFirst()
        .orElse(null);
  }

  public String getTooltipText(Node node) {
    return Optional.ofNullable(getTooltip(node)).orElseThrow().getText();
  }
}
