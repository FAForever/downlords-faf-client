package com.faforever.client.news;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.ThemeService;
import com.google.common.eventbus.EventBus;
import com.google.common.io.CharStreams;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class NewsController {

  private static final ClassPathResource NEWS_DETAIL_HTML_RESOURCE = new ClassPathResource("/theme/news_detail.html");

  @FXML
  Pane newsRoot;
  @FXML
  Pane newsListPane;
  @FXML
  WebView newsDetailWebView;

  @Resource
  ApplicationContext applicationContext;
  @Resource
  PreferencesService preferencesService;
  @Resource
  I18n i18n;
  @Resource
  NewsService newsService;
  @Resource
  ThemeService themeService;
  @Resource
  EventBus eventBus;
  @Resource
  WebViewConfigurer webViewConfigurer;

  public void setUpIfNecessary() {
    if (!newsListPane.getChildren().isEmpty()) {
      return;
    }

    newsDetailWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(newsDetailWebView);
    themeService.registerWebView(newsDetailWebView);

    boolean firstItemSelected = false;

    List<NewsItem> newsItems = newsService.fetchNews();
    for (NewsItem newsItem : newsItems) {
      NewsListItemController newsListItemController = applicationContext.getBean(NewsListItemController.class);
      newsListItemController.setNewsItem(newsItem);
      newsListItemController.setOnItemSelectedListener(this::displayNewsItem);

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
