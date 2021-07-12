package com.faforever.client.replay;

import com.faforever.client.game.Game;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.VictoryCondition;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalReplayInfoTest extends ServiceTest {
  private static final String host = "Test";
  private static final int uid = 1234;
  private static final String title = "Test Game";
  private static final String mapname = "test_map";
  private static final GameStatus state = GameStatus.CLOSED;
  private static final VictoryCondition gameType = VictoryCondition.DEMORALIZATION;
  private static final String featuredMod = "faf";
  private static final int maxPlayers = 6;
  private static final int numPlayers = 4;
  private static final Map<String, String> simMods = Map.of("test", "simtest");
  private static final Map<String, List<String>> teams = Map.of("Army 1", List.of("P1", "P2"), "Army 2", List.of("P3", "P4"));
  private static final Map<String, Integer> featuredModVersions = Map.of("faf", 1);

  private LocalReplayInfo instance;
  private Game game;

  @BeforeEach
  public void setUp() throws Exception {
    game = new Game();
    game.setHost(host);
    game.setId(uid);
    game.setTitle(title);
    game.setMapFolderName(mapname);
    game.setStatus(state);
    game.setVictoryCondition(gameType);
    game.setFeaturedMod(featuredMod);
    game.setMaxPlayers(maxPlayers);
    game.setNumPlayers(numPlayers);
    game.setSimMods(simMods);
    game.setTeams(teams);
    game.setFeaturedModVersions(featuredModVersions);

    instance = new LocalReplayInfo();
  }

  @Test
  public void updateData(){
    instance.updateFromGameInfoBean(game);
    assertEquals(instance.getHost(), host);
    assertEquals((int) instance.getUid(), uid);
    assertEquals(instance.getTitle(), title);
    assertEquals(instance.getMapname(), mapname);
    assertEquals(instance.getState(), state);
    assertEquals(instance.getGameType(), gameType);
    assertEquals(instance.getFeaturedMod(), featuredMod);
    assertEquals((int) instance.getMaxPlayers(), maxPlayers);
    assertEquals((int) instance.getNumPlayers(), numPlayers);
    assertEquals(instance.getSimMods(), simMods);
    assertEquals(instance.getTeams(), teams);
    assertEquals(instance.getFeaturedModVersions(), featuredModVersions);
  }
}
