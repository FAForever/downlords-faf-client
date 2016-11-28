package com.faforever.client.news;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import com.google.common.io.CharStreams;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewsController extends AbstractViewController<Node> {

  private static final ClassPathResource NEWS_DETAIL_HTML_RESOURCE = new ClassPathResource("/theme/news_detail.html");

  public Pane newsRoot;
  public Pane newsListPane;
  public WebView newsDetailWebView;

  @Inject
  PreferencesService preferencesService;
  @Inject
  I18n i18n;
  @Inject
  NewsService newsService;
  @Inject
  UiService uiService;
  @Inject
  EventBus eventBus;
  @Inject
  WebViewConfigurer webViewConfigurer;

  @Override
  public void onDisplay() {
    if (!newsListPane.getChildren().isEmpty()) {
      return;
    }

    newsDetailWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(newsDetailWebView);

    boolean firstItemSelected = false;

    List<NewsItem> newsItems = newsService.fetchNews();
    for (NewsItem newsItem : newsItems) {
      NewsListItemController newsListItemController = uiService.loadFxml("theme/news_list_item.fxml");
      newsListItemController.setNewsItem(newsItem);
      newsListItemController.setOnItemSelectedListener((item) -> {
        newsListPane.getChildren().forEach(node -> node.pseudoClassStateChanged(NewsListItemController.SELECTED_PSEUDO_CLASS, false));
        displayNewsItem(item);
        newsListItemController.getRoot().pseudoClassStateChanged(NewsListItemController.SELECTED_PSEUDO_CLASS, true);
      });

      newsListPane.getChildren().add(newsListItemController.getRoot());

      if (!firstItemSelected) {
        preferencesService.getPreferences().getNews().setLastReadNewsUrl(newsItem.getLink());
        preferencesService.storeInBackground();
        newsListItemController.onMouseClicked();
        firstItemSelected = true;
      }
    }
  }

  private void displayNewsItem(NewsItem newsItem) {
    eventBus.post(new UnreadNewsEvent(false));

    try (Reader reader = new InputStreamReader(NEWS_DETAIL_HTML_RESOURCE.getInputStream())) {
      String html = CharStreams.toString(reader);

      html = html.replace("{title}", newsItem.getTitle())
          .replace("{content}", newsItem.getContent())
          .replace("{authored}", i18n.get("news.authoredFormat", newsItem.getAuthor(), newsItem.getDate()));

      newsDetailWebView.getEngine().loadContent(html);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Node getRoot() {
    return newsRoot;
  }
}
