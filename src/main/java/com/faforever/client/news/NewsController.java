package com.faforever.client.news;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.ShowLadderMapsEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
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

  private static final ClassPathResource NEWS_DETAIL_HTML_RESOURCE = new ClassPathResource("/theme/news_detail.html");
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final NewsService newsService;
  private final UiService uiService;
  private final EventBus eventBus;
  private final WebViewConfigurer webViewConfigurer;
  public Pane newsRoot;
  public WebView newsDetailWebView;
  public Button showLadderMapsButton;
  public ListView<NewsItem> newsListView;
  public Control loadingIndicator;
  private ChangeListener<Boolean> loadingIndicatorListener;

  public NewsController(PreferencesService preferencesService, I18n i18n, NewsService newsService, UiService uiService, EventBus eventBus, WebViewConfigurer webViewConfigurer) {
    this.preferencesService = preferencesService;
    this.i18n = i18n;
    this.newsService = newsService;
    this.uiService = uiService;
    this.eventBus = eventBus;
    this.webViewConfigurer = webViewConfigurer;

    loadingIndicatorListener = (observable, oldValue, newValue)
        -> loadingIndicator.getParent().getChildrenUnmodifiable().stream()
        .filter(node -> node != loadingIndicator)
        .forEach(node -> node.setVisible(!newValue));
  }

  @Override
  public void initialize() {
    newsListView.setCellFactory(param -> new NewsItemListCell(uiService));
    newsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> displayNewsItem(newValue));

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
  public void onDisplay(NavigateEvent navigateEvent) {
    if (!newsListView.getItems().isEmpty()) {
      return;
    }

    showLadderMapsButton.managedProperty().bind(showLadderMapsButton.visibleProperty());
    showLadderMapsButton.setVisible(false);
    newsDetailWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(newsDetailWebView);

    onLoadingStart();
    newsService.fetchNews().thenAccept(newsItems -> {
      newsListView.getItems().setAll(newsItems);
      onLoadingStop();
      if (!newsItems.isEmpty()) {
        NewsItem mostRecentItem = newsItems.get(0);
        preferencesService.getPreferences().getNews().setLastReadNewsUrl(mostRecentItem.getLink());
        preferencesService.storeInBackground();
      }
      newsListView.getSelectionModel().selectFirst();
    });
  }

  @SneakyThrows
  private void displayNewsItem(NewsItem newsItem) {
    showLadderMapsButton.setVisible(newsItem.getNewsCategory().equals(NewsCategory.LADDER));
    eventBus.post(new UnreadNewsEvent(false));

    try (Reader reader = new InputStreamReader(NEWS_DETAIL_HTML_RESOURCE.getInputStream())) {
      String html = CharStreams.toString(reader).replace("{title}", newsItem.getTitle())
          .replace("{content}", newsItem.getContent())
          .replace("{authored}", i18n.get("news.authoredFormat", newsItem.getAuthor(), newsItem.getDate()));

      Platform.runLater(() -> newsDetailWebView.getEngine().loadContent(html));
    }
  }

  public Node getRoot() {
    return newsRoot;
  }

  public void showLadderMaps() {
    eventBus.post(new ShowLadderMapsEvent());
  }
}
