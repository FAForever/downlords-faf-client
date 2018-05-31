package com.faforever.client.news;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.eventbus.EventBus;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
public class NewsService {

  /** The delay (in seconds) between polling for new news. */
  private static final long POLL_DELAY = Duration.ofMinutes(10).toMillis();

  private final String newsFeedUrl;

  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final TaskScheduler taskScheduler;

  public NewsService(ClientProperties clientProperties, PreferencesService preferencesService, EventBus eventBus,
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
    fetchNews().thenAccept(newsItems -> newsItems.stream().findFirst()
        .ifPresent(newsItem -> {
          String lastReadNewsUrl = preferencesService.getPreferences().getNews().getLastReadNewsUrl();
          if (!Objects.equals(newsItem.getLink(), lastReadNewsUrl)) {
            eventBus.post(new UnreadNewsEvent(true));
          }
        }));
  }

  @Async
  public CompletableFuture<List<NewsItem>> fetchNews() {
    return CompletableFuture.completedFuture(
        noCatch(() -> new SyndFeedInput().build(new XmlReader(new URL(newsFeedUrl)))).getEntries().stream()
            .map(this::toNewsItem)
            .sorted(Comparator.comparing(NewsItem::getDate).reversed())
            .collect(Collectors.toList()));
  }

  private NewsItem toNewsItem(SyndEntry syndEntry) {
    String author = syndEntry.getAuthor();
    String link = syndEntry.getLink();
    String title = syndEntry.getTitle();
    String content = syndEntry.getContents().get(0).getValue();
    Date publishedDate = syndEntry.getPublishedDate();

    NewsCategory newsCategory = syndEntry.getCategories().stream()
        .filter(Objects::nonNull)
        .findFirst()
        .map(SyndCategory::getName)
        .map(NewsCategory::fromString)
        .orElse(NewsCategory.UNCATEGORIZED);

    return new NewsItem(author, link, title, content, publishedDate, newsCategory);
  }
}
