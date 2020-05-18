package com.faforever.client.news;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.ShowLadderMapsEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.CharStreams;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import lombok.SneakyThrows;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewsController extends AbstractViewController<Node> {

  private final EventBus eventBus;
  private final WebViewConfigurer webViewConfigurer;
  public Pane newsRoot;
  public WebView newsWebView;
  public Control loadingIndicator;
  private ChangeListener<Boolean> loadingIndicatorListener;

  public NewsController(EventBus eventBus, WebViewConfigurer webViewConfigurer) {
    this.eventBus = eventBus;
    this.webViewConfigurer = webViewConfigurer;

    loadingIndicatorListener = (observable, oldValue, newValue)
        -> loadingIndicator.getParent().getChildrenUnmodifiable().stream()
        .filter(node -> node != loadingIndicator)
        .forEach(node -> node.setVisible(!newValue));
    eventBus.register(this);
  }

  @Override
  public void initialize() {

    loadingIndicator.managedProperty().bind(loadingIndicator.visibleProperty());
    loadingIndicator.visibleProperty().addListener(loadingIndicatorListener);
    loadingIndicatorListener.changed(loadingIndicator.visibleProperty(), null, true);

    loadingIndicator.getParent().getChildrenUnmodifiable()
        .forEach(node -> node.managedProperty().bind(node.visibleProperty()));
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
    newsWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(newsWebView);
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
      newsWebView.getEngine().load("https://www.faforever.com/news");
      onLoadingStop();
    });
  }

  public Node getRoot() {
    return newsRoot;
  }

}
