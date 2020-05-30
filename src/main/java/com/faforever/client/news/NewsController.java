package com.faforever.client.news;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Website;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.main.event.NavigateEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewsController extends AbstractViewController<Node> {
  private final EventBus eventBus;
  private final WebViewConfigurer webViewConfigurer;
  private final ClientProperties clientProperties;
  public Pane newsRoot;
  public WebView newsWebView;
  public Control loadingIndicator;
  private ChangeListener<Boolean> loadingIndicatorListener;

  public NewsController(EventBus eventBus, WebViewConfigurer webViewConfigurer, ClientProperties clientProperties) {
    this.eventBus = eventBus;
    this.webViewConfigurer = webViewConfigurer;
    this.clientProperties = clientProperties;

    loadingIndicatorListener = (observable, oldValue, newValue)
        -> loadingIndicator.getParent().getChildrenUnmodifiable().stream()
        .filter(node -> node != loadingIndicator)
        .forEach(node -> node.setVisible(!newValue));
    eventBus.register(this);
  }

  @Override
  public void initialize() {
    newsWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(newsWebView);

    loadingIndicator.managedProperty().bind(loadingIndicator.visibleProperty());
    loadingIndicator.visibleProperty().addListener(loadingIndicatorListener);
    loadingIndicatorListener.changed(loadingIndicator.visibleProperty(), null, true);

    loadingIndicator.getParent().getChildrenUnmodifiable()
        .forEach(node -> node.managedProperty().bind(node.visibleProperty()));
    newsWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        onLoadingStop();
      }
    });
  }

  private void onLoadingStart() {
    Platform.runLater(() -> loadingIndicator.setVisible(true));
  }

  private void onLoadingStop() {
    Platform.runLater(() -> loadingIndicator.setVisible(false));
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    eventBus.post(new UnreadNewsEvent(false));
    onLoadingStart();
    loadNews();
  }

  @Subscribe
  public void onUnreadNewsEvent(UnreadNewsEvent unreadNewsEvent) {
    if (unreadNewsEvent.hasUnreadNews()) {
      onLoadingStart();
      loadNews();
    }
  }


  private void loadNews() {
    Platform.runLater(() -> {
      Website website = clientProperties.getWebsite();
      newsWebView.getEngine().load(website.getNewsHubUrl());
    });
  }

  public Node getRoot() {
    return newsRoot;
  }

}
