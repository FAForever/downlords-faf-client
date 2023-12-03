package com.faforever.client.fx;

import com.faforever.client.theme.ThemeService;
import com.google.common.base.Strings;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class DualStringListCellController extends NodeController<Node> {
  private final ThemeService themeService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public HBox root;
  public Label left;
  public Label right;
  public Tooltip tooltip;

  private CompletableFuture<WebView> webViewInitializeFuture;

  @Override
  protected void onInitialize() {
    webViewInitializeFuture = CompletableFuture.supplyAsync(() -> {
      WebView webView = new WebView();
      themeService.registerWebView(webView);
      tooltip.setGraphic(webView);
      return webView;
    }, fxApplicationThreadExecutor);
  }

  public void setLeftText(String apply) {
    left.setText(apply);
  }

  public void setRightText(String apply) { right.setText(apply); }

  public void setWebViewToolTip(String content) {
    if (!Strings.isNullOrEmpty(content)) {
      webViewInitializeFuture.thenAcceptAsync(webView -> webView.getEngine().loadContent(content),
                                              fxApplicationThreadExecutor);
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
