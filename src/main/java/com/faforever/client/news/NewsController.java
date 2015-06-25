package com.faforever.client.news;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.JavaFxUtil;
import com.google.common.io.CharStreams;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Date;

public class NewsController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final ClassPathResource NEWS_DETAIL_HTML_RESOURCE = new ClassPathResource("/themes/default/news_detail.html");

  @FXML
  Pane newsRoot;

  @FXML
  Pane newsListPane;

  @FXML
  WebView newsDetailWebView;

  @Autowired
  Environment environment;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  I18n i18n;

  public void setUpIfNecessary() {
    if (!newsListPane.getChildren().isEmpty()) {
      return;
    }

    newsDetailWebView.setContextMenuEnabled(false);
    JavaFxUtil.configureWebView(newsDetailWebView, preferencesService);

    SyndFeedInput input = new SyndFeedInput();
    String newsFeedUrl = environment.getProperty("newsFeedUrl");
    try {
      SyndFeed feed = input.build(new XmlReader(new URL(newsFeedUrl)));

      boolean firstItemSelected = false;

      for (SyndEntry syndEntry : feed.getEntries()) {
        String author = syndEntry.getAuthor();
        String link = syndEntry.getLink();
        String title = syndEntry.getTitle();
        String content = syndEntry.getContents().get(0).getValue();
        Date publishedDate = syndEntry.getPublishedDate();

        NewsListItemController newsListItemController = applicationContext.getBean(NewsListItemController.class);
        newsListItemController.setNewsItem(new NewsItem(author, link, title, content, publishedDate));
        newsListItemController.setOnItemSelectedListener(this::displayNewsItem);

        newsListPane.getChildren().add(newsListItemController.getRoot());

        if (!firstItemSelected) {
          newsListItemController.onMouseClicked();
          firstItemSelected = true;
        }
      }

    } catch (FeedException | IOException e) {
      // FIXME display error to user
      logger.warn("Could not load news feed", e);
    }
  }

  private void displayNewsItem(NewsItem newsItem) {
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
