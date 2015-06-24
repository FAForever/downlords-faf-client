package com.faforever.client.news;

import com.faforever.client.preferences.PreferencesService;
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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Date;

public class NewsController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  public void setUpIfNecessary() {
    if (!newsListPane.getChildren().isEmpty()) {
      return;
    }

    SyndFeedInput input = new SyndFeedInput();
    String newsFeedUrl = environment.getProperty("newsFeedUrl");
    try {
      SyndFeed feed = input.build(new XmlReader(new URL(newsFeedUrl)));

      boolean firstItemSelected = false;

      for (SyndEntry syndEntry : feed.getEntries()) {
        String author = syndEntry.getAuthor();
        String link = syndEntry.getLink();
        String title = syndEntry.getTitle();
        String description = syndEntry.getDescription().getValue();
        Date publishedDate = syndEntry.getPublishedDate();

        NewsListItemController newsListItemController = applicationContext.getBean(NewsListItemController.class);
        newsListItemController.setNewsItem(new NewsItem(author, link, title, author, publishedDate));
        newsListItemController.setOnItemSelectedListener(this::onNewsItemSelected);

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

  private void onNewsItemSelected(NewsItem newsItem) {
    newsDetailWebView.getEngine().load(newsItem.getLink());
  }

  public Node getRoot() {
    return newsRoot;
  }
}
