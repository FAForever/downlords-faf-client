package com.faforever.client.news;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Website;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.WebViewConfigurer;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class NewsController extends NodeController<Node> {
  private final WebViewConfigurer webViewConfigurer;
  private final ClientProperties clientProperties;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Pane newsRoot;
  public Control loadingIndicator;

  private WebView newsWebView;

  @Override
  protected void onInitialize() {

    loadingIndicator.managedProperty().bind(loadingIndicator.visibleProperty());
    loadingIndicator.visibleProperty()
                    .subscribe(newValue -> loadingIndicator.getParent()
                                                           .getChildrenUnmodifiable()
                                                           .stream()
                                                           .filter(node1 -> node1 != loadingIndicator)
                                                           .forEach(node1 -> node1.setVisible(!newValue)));

    loadingIndicator.getParent().getChildrenUnmodifiable()
        .forEach(node -> node.managedProperty().bind(node.visibleProperty()));

    onLoadingStart();

    CompletableFuture.runAsync(() -> {
      newsWebView = new WebView();
      newsWebView.setVisible(false);
      newsRoot.getChildren().add(newsWebView);
      newsWebView.setContextMenuEnabled(false);
      webViewConfigurer.configureWebView(newsWebView);
      newsWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
        if (newState == Worker.State.SUCCEEDED) {
          onLoadingStop();
        }
      });
      Website website = clientProperties.getWebsite();
      newsWebView.getEngine().load(website.getNewsHubUrl());
    }, fxApplicationThreadExecutor);
  }

  private void onLoadingStart() {
    fxApplicationThreadExecutor.execute(() -> loadingIndicator.setVisible(true));
  }

  private void onLoadingStop() {
    fxApplicationThreadExecutor.execute(() -> loadingIndicator.setVisible(false));
  }

  @Override
  public Node getRoot() {
    return newsRoot;
  }

}
