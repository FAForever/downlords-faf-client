package com.faforever.client.stats;

import com.faforever.client.api.RatingType;
import com.faforever.client.remote.FafService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

public class StatisticsServiceImplTest extends AbstractPlainJavaFxTest {

  @Mock
  FafService fafService;

  private StatisticsServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new StatisticsServiceImpl();
    instance.fafService = fafService;
  }

  @Test
  public void testGetStatisticsForPlayer() throws Exception {
    instance.getRatingHistory(RatingType.GLOBAL, 123);
    verify(fafService).getRatingHistory(RatingType.GLOBAL, 123);
  }
}
