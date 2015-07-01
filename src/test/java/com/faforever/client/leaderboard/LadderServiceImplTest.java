package com.faforever.client.leaderboard;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.util.Callback;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LadderServiceImplTest {

  private LadderServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new LadderServiceImpl();
    instance.lobbyServerAccessor = mock(LobbyServerAccessor.class);
  }

  @Test
  public void testGetLadderInfo() throws Exception {
    Callback<List<LadderEntryBean>> callback = mock(Callback.class);

    instance.getLadderInfo(callback);

    verify(instance.lobbyServerAccessor).requestLadderInfoInBackground(callback);
  }
}
