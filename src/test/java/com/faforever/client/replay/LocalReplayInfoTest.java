package com.faforever.client.replay;

import com.faforever.client.game.Game;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.VictoryCondition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LocalReplayInfoTest {
  private static String host = "Test";
  private static int uid = 1234;
  private static String title = "Test Game";
  private static String mapname = "test_map";
  private static GameStatus state = GameStatus.CLOSED;
  private static VictoryCondition gameType = VictoryCondition.DEMORALIZATION;
  private static String featuredMod = "faf";
  private static int maxPlayers = 6;
  private static int numPlayers = 4;
  private static ObservableMap<String, String> simMods = FXCollections.observableMap(Map.of("test", "simtest"));
  private static ObservableMap<String, List<String>> teams = FXCollections.observableMap(Map.of("Army 1", List.of("P1", "P2"),"Army 2", List.of("P3", "P4")));;
  private static ObservableMap<String, Integer> featuredModVersions = FXCollections.observableMap(Map.of("faf", 1));

  private LocalReplayInfo instance;
  private Game game;

  @Before
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
