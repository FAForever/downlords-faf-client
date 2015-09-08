package com.faforever.client.news;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LegacyNewsService implements NewsService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  @Override
  public List<NewsItem> fetchNews() {
    List<NewsItem> result = new ArrayList<>();

    SyndFeedInput input = new SyndFeedInput();
    String newsFeedUrl = environment.getProperty("newsFeedUrl");

    try {
      SyndFeed feed = input.build(new XmlReader(new URL(newsFeedUrl)));

      for (SyndEntry syndEntry : feed.getEntries()) {
        String author = syndEntry.getAuthor();
        String link = syndEntry.getLink();
        String title = syndEntry.getTitle();
        String content = syndEntry.getContents().get(0).getValue();
        Date publishedDate = syndEntry.getPublishedDate();

        result.add(new NewsItem(author, link, title, content, publishedDate));
      }
    } catch (FeedException | IOException e) {
      throw new RuntimeException(e);
    }

    return result;
  }
}
