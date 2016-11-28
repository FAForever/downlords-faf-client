package com.faforever.client.news;

import com.faforever.client.config.CacheNames;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.eventbus.EventBus;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
public class LegacyNewsService implements NewsService {

  /**
   * The delay (in seconds) between polling for new news.
   */
  private static final long POLL_DELAY = 1800;

  @Value("${newsFeedUrl}")
  String newsFeedUrl;

  @Inject
  PreferencesService preferencesService;
  @Inject
  EventBus eventBus;
  @Inject
  ScheduledExecutorService scheduledExecutorService;

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
    scheduledExecutorService.scheduleWithFixedDelay(this::pollForNews, 5, POLL_DELAY, TimeUnit.SECONDS);
  }

  private void pollForNews() {
    fetchNews().stream().findFirst()
        .ifPresent(newsItem -> {
          String lastReadNewsUrl = preferencesService.getPreferences().getNews().getLastReadNewsUrl();
          if (!Objects.equals(newsItem.getLink(), lastReadNewsUrl)) {
            eventBus.post(new UnreadNewsEvent(true));
          }
        });
  }

  @Override
  @Cacheable(CacheNames.NEWS)
  public List<NewsItem> fetchNews() {
    List<NewsItem> result = new ArrayList<>();

    SyndFeedInput input = new SyndFeedInput();

    SyndFeed feed = noCatch(() -> input.build(new XmlReader(new URL(newsFeedUrl))));

    for (SyndEntry syndEntry : feed.getEntries()) {
      String author = syndEntry.getAuthor();
      String link = syndEntry.getLink();
      String title = syndEntry.getTitle();
      String content = syndEntry.getContents().get(0).getValue();
      Date publishedDate = syndEntry.getPublishedDate();

      result.add(new NewsItem(author, link, title, content, publishedDate));
    }

    return result;
  }
}
