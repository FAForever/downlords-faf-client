package com.faforever.client.fx;

import com.faforever.client.theme.ThemeService;
import com.google.common.base.Strings;
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
public class DualStringListCellController extends NodeController<Node> {
  private final ThemeService themeService;

  public HBox root;
  public Label left;
  public Label right;

  private String toolTipContent;

  public void setLeftText(String apply) {
    left.setText(apply);
  }

  public void setRightText(String apply) { right.setText(apply); }

  /**
   * The WebView is only created once the tooltip is shown, since every WebView instance is expensive.
   */
  public void setWebViewToolTip(String apply) {
    toolTipContent = apply;
    if (Strings.isNullOrEmpty(apply)) {
      left.setTooltip(null);
      return;
    }

    if (left.getTooltip() == null) {
      Tooltip tooltip = new Tooltip();
      tooltip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      tooltip.setShowDelay(Duration.seconds(.5));
      tooltip.setHideDelay(Duration.seconds(2));
      tooltip.setShowDuration(Duration.seconds(30));
      tooltip.setOnShowing(event -> {
        WebView webView = (WebView) tooltip.getGraphic();
        if (webView == null) {
          webView = new WebView();
          webView.setPrefSize(300, 250);
          themeService.registerWebView(webView);
          tooltip.setGraphic(webView);
        }
        webView.getEngine().loadContent(toolTipContent);
      });
      left.setTooltip(tooltip);
    }
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
