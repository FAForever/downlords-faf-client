package com.faforever.client.fx;

import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class DualStringListCellController implements Controller<Node> {
  private final UiService uiService;
  public HBox root;
  public Label left;
  public Label right;

  public void setLeftText(String apply) {
    left.setText(apply);
  }

  public void setLeftTextOver(String apply) {
    final WebView web = new WebView();
    uiService.registerWebView(web);
    web.setPrefSize(web.getPrefWidth() / 2, web.getPrefHeight() / 2);
    web.getEngine().loadContent(apply);
    final Tooltip  tip = new Tooltip();
    tip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    tip.setGraphic(web);
    tip.setShowDelay(Duration.ZERO);
    tip.setHideDelay(Duration.seconds(3));
    left.setTooltip(tip);
  }

  public void setRightText(String apply) {
    right.setText(apply);
  }

  public void applyFont(Font font) {
    left.setFont(font);
    right.setFont(font);
  }

  public void applyStyleClass(String styleClasses) {
    root.getStyleClass().addAll(styleClasses);
  }

  @Override
  public Node getRoot() {
    return root;
  }
}
