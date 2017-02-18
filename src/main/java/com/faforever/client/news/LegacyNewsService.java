package com.faforever.client.news;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.eventbus.EventBus;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
public class LegacyNewsService implements NewsService {

  /**
   * The delay (in seconds) between polling for new news.
   */
  private static final long POLL_DELAY = Duration.ofMinutes(5).toMillis();

  private final String newsFeedUrl;

  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final TaskScheduler taskScheduler;

  @Inject
  public LegacyNewsService(ClientProperties clientProperties, PreferencesService preferencesService, EventBus eventBus,
                           TaskScheduler taskScheduler) {
    this.newsFeedUrl = clientProperties.getNews().getFeedUrl();

    this.preferencesService = preferencesService;
    this.eventBus = eventBus;
    this.taskScheduler = taskScheduler;
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
    taskScheduler.scheduleWithFixedDelay(this::pollForNews, Date.from(Instant.now().plusSeconds(5)), POLL_DELAY);
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
