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
import org.jsoup.Jsoup;
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

  @Autowired
  Environment environment;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  PreferencesService preferencesService;

  public void setUpIfNecessary() {
    if (!newsRoot.getChildren().isEmpty()) {
      return;
    }

    SyndFeedInput input = new SyndFeedInput();
    String newsFeedUrl = environment.getProperty("newsFeedUrl");
    try {
      SyndFeed feed = input.build(new XmlReader(new URL(newsFeedUrl)));

      for (SyndEntry syndEntry : feed.getEntries()) {
        String author = syndEntry.getAuthor();
        String link = syndEntry.getLink();
        String title = syndEntry.getTitle();
        String description = Jsoup.parse(syndEntry.getDescription().getValue()).text();
        Date publishedDate = syndEntry.getPublishedDate();

        NewsTileController newsTileController = applicationContext.getBean(NewsTileController.class);
        newsTileController.setAuthored(author, publishedDate);
        newsTileController.setLink(link);
        newsTileController.setTitle(title);
        newsTileController.setDescription(description);

        newsRoot.getChildren().add(newsTileController.getRoot());
      }

    } catch (FeedException | IOException e) {
      // FIXME display error to user
      logger.warn("Could not load news feed", e);
    }
  }

  public Node getRoot() {
    return newsRoot;
  }
}
