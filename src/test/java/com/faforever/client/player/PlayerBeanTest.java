package com.faforever.client.player;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerGameStatus;
import com.faforever.client.test.DomainTest;
import com.faforever.commons.lobby.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;


public class PlayerBeanTest extends DomainTest {
  private PlayerBean instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = PlayerBeanBuilder.create().defaultValues().get();
  }

  @Test
  public void testStateChangesOnGameBeingSet() {
    List<List<Object>> callParameters = Arrays.asList(Arrays.asList(GameStatus.OPEN, PlayerGameStatus.HOSTING, "junit"),
                                                      Arrays.asList(GameStatus.OPEN, PlayerGameStatus.LOBBYING,
                                                                    "whatever"),
                                                      Arrays.asList(GameStatus.PLAYING, PlayerGameStatus.PLAYING,
                                                                    "junit"),
                                                      Arrays.asList(GameStatus.CLOSED, PlayerGameStatus.IDLE, "junit"),
                                                      Arrays.asList(null, PlayerGameStatus.IDLE, "junit")
    );

    callParameters.forEach(objects -> checkStatusSetCorrectly((GameStatus) objects.getFirst(),
                                                              (PlayerGameStatus) objects.get(1),
                                                              (String) objects.get(2))
    );

  }

  private void checkStatusSetCorrectly(GameStatus gameStatus, PlayerGameStatus playerGameStatus, String playerName) {
    GameBean game = null;
    if (gameStatus != null) {
      game = GameBeanBuilder.create()
          .defaultValues()
          .status(gameStatus)
          .host(playerName)
          .get();
    }
    instance.setGame(game);
    assertSame(instance.getGameStatus(), playerGameStatus);
  }

  @Test
  public void testPlayerStateChangedOnGameStatusChanged() {
    PlayerBean player1 = PlayerBeanBuilder.create().username("unit1").defaultValues().game(null).get();
    PlayerBean player2 = PlayerBeanBuilder.create().username("unit2").defaultValues().game(null).get();
    assertSame(player1.getGameStatus(), PlayerGameStatus.IDLE);
    assertSame(player2.getGameStatus(), PlayerGameStatus.IDLE);
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    player1.setGame(game);
    player2.setGame(game);
    assertSame(player1.getGameStatus(), PlayerGameStatus.LOBBYING);
    assertSame(player2.getGameStatus(), PlayerGameStatus.LOBBYING);
    game.setStatus(GameStatus.PLAYING);
    assertSame(player1.getGameStatus(), PlayerGameStatus.PLAYING);
    assertSame(player2.getGameStatus(), PlayerGameStatus.PLAYING);
    game.setStatus(GameStatus.CLOSED);
    assertSame(player1.getGameStatus(), PlayerGameStatus.IDLE);
    assertSame(player2.getGameStatus(), PlayerGameStatus.IDLE);
    game.setStatus(GameStatus.PLAYING);
    assertSame(player1.getGameStatus(), PlayerGameStatus.PLAYING);
    assertSame(player2.getGameStatus(), PlayerGameStatus.PLAYING);
    player1.setGame(null);
    player2.setGame(null);
    assertSame(player1.getGameStatus(), PlayerGameStatus.IDLE);
    assertSame(player2.getGameStatus(), PlayerGameStatus.IDLE);
  }
}