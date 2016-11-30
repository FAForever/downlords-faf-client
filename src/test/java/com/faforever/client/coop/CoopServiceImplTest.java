package com.faforever.client.coop;

import com.faforever.client.remote.FafService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;


public class CoopServiceImplTest {

  private CoopServiceImpl instance;

  @Mock
  private FafService fafService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new CoopServiceImpl();
    instance.fafService = fafService;
  }

  @Test
  public void getCoopMaps() throws Exception {
    instance.getMissions();

    verify(fafService).getCoopMaps();
  }
}
