package com.faforever.client.stats;

import com.faforever.client.config.CacheNames;
import com.faforever.client.legacy.StatisticsServerAccessor;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.stats.domain.GameStats;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.oxm.Unmarshaller;
import org.w3c.tidy.Tidy;

import javax.annotation.Resource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;

public class StatisticsServiceImpl implements StatisticsService {

  @Resource
  Unmarshaller unmarshaller;
  @Resource
  StatisticsServerAccessor statisticsServerAccessor;

  @Override
  @Cacheable(value = CacheNames.STATISTICS, key = "#type + #username")
  public CompletableFuture<PlayerStatistics> getStatisticsForPlayer(StatisticsType type, String username) {
    return statisticsServerAccessor.requestPlayerStatistics(username, type);
  }

  @Override
  public GameStats parseStatistics(String xmlString) {
    StringWriter out = new StringWriter();

    Tidy tidy = new Tidy();
    tidy.setXmlTags(true);
    tidy.setXmlOut(true);
    tidy.setQuiet(true);
    tidy.setShowWarnings(false);
    tidy.parse(new StringReader(xmlString), out);

    try {
      return (GameStats) unmarshaller.unmarshal(new StreamSource(new StringReader(out.toString())));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
