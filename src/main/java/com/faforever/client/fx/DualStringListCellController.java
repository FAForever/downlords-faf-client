package com.faforever.client.fx;

import com.faforever.client.theme.UiService;
import com.google.common.base.Strings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
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
  public WebView webViewToolTip;

  public void setLeftText(String apply) {
    left.setText(apply);
  }

  public void setRightText(String apply) { right.setText(apply); }

  public void setWebViewToolTip(String apply) {
    if (!Strings.isNullOrEmpty(apply)) {
      uiService.registerWebView(webViewToolTip);
      webViewToolTip.getEngine().loadContent(apply);
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
