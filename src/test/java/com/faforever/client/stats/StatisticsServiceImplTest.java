package com.faforever.client.stats;

import com.faforever.client.api.RatingType;
import com.faforever.client.remote.FafService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class StatisticsServiceImplTest {

  @Mock
  private
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
