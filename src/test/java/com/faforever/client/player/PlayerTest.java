package com.faforever.client.player;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;


public class PlayerTest extends ServiceTest {
  private PlayerBean instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = PlayerBeanBuilder.create().defaultValues().get();
  }

  @Test
  public void testStateChangesOnGameBeingSet() {
    List<List<Object>> callParameters = Arrays.asList(
        Arrays.asList(GameStatus.OPEN, PlayerStatus.HOSTING, "junit"),
        Arrays.asList(GameStatus.OPEN, PlayerStatus.LOBBYING, "whatever"),
        Arrays.asList(GameStatus.PLAYING, PlayerStatus.PLAYING, "junit"),
        Arrays.asList(GameStatus.CLOSED, PlayerStatus.IDLE, "junit"),
        Arrays.asList(null, PlayerStatus.IDLE, "junit")
    );

    callParameters.forEach(objects -> checkStatusSetCorrectly(
        (GameStatus) objects.get(0), (PlayerStatus) objects.get(1), (String) objects.get(2))
    );

  }

  private void checkStatusSetCorrectly(GameStatus gameStatus, PlayerStatus playerStatus, String playerName) {
    GameBean game = null;
    if (gameStatus != null) {
      game = new GameBean();
      game.setStatus(gameStatus);
      game.setHost(playerName);
    }
    instance.setGame(game);
    assertSame(instance.getStatus(), playerStatus);
  }

  @Test
  public void testPlayerStateChangedOnGameStatusChanged() {
    GameBean game = new GameBean();
    game.setStatus(GameStatus.PLAYING);
    instance.setGame(game);
    assertSame(instance.getStatus(), PlayerStatus.PLAYING);
    game.setStatus(GameStatus.CLOSED);
    instance.setGame(null);
    assertSame(instance.getStatus(), PlayerStatus.IDLE);
  }
}