package com.faforever.client.leaderboard;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.util.Callback;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LeaderboardServiceImplTest {

  private LeaderboardServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new LeaderboardServiceImpl();
    instance.lobbyServerAccessor = mock(LobbyServerAccessor.class);
  }

  @Test
  public void testGetLadderInfo() throws Exception {
    @SuppressWarnings("unchecked")
    Callback<List<LeaderboardEntryBean>> callback = mock(Callback.class);

    instance.getLadderInfo(callback);

    verify(instance.lobbyServerAccessor).requestLadderInfoInBackground(callback);
  }
}
