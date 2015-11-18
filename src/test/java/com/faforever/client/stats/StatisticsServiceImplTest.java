package com.faforever.client.stats;

import com.faforever.client.legacy.StatisticsServerAccessor;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

public class StatisticsServiceImplTest extends AbstractPlainJavaFxTest {

  @Mock
  StatisticsServerAccessor statisticsServerAccessor;

  private StatisticsServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new StatisticsServiceImpl();
    instance.statisticsServerAccessor = statisticsServerAccessor;
  }

  @Test
  public void testGetStatisticsForPlayer() throws Exception {
    instance.getStatisticsForPlayer(StatisticsType.GLOBAL_90_DAYS, "junit");
    verify(statisticsServerAccessor).requestPlayerStatistics(StatisticsType.GLOBAL_90_DAYS, "junit");
  }
}
