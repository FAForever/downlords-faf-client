package com.faforever.client.game;

import com.faforever.client.player.Player;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class GameRangePredicateTest {

  private Player validPlayer;
  private GameRangePredicate predicate;
  private Game game;

  @Before
  public void setUp() {
    validPlayer = new Player("");
    validPlayer.setGlobalRatingMean(1400);
    validPlayer.setGlobalRatingDeviation(100); //this should result in a global rating of 1100
    game = new Game();
    game.setEnforceRating(true);

    predicate = new GameRangePredicate(Optional.of(validPlayer));
  }

  @Test
  public void testMatchAlwaysMatchOnNoPlayer() {
    //Arrange
    GameRangePredicate rangePredicate = new GameRangePredicate(Optional.empty());
    ArrayList<Game> games = new ArrayList<>();
    games.add(game);

    Game game1 = new Game();
    game1.setEnforceRating(true);
    game1.setMinRating(100);
    game1.setMaxRating(0);
    games.add(game1);

    //Act
    //Assert
    for(Game game : games) {
      Assert.assertTrue(String.format("%s > ?? < %s", game.getMaxRating(), game.getMinRating()), rangePredicate.test(game));
    }
  }

  @Test
  public void testAlwaysMatchWhenNotEnforcing() {
    //Arrange
    game.setEnforceRating(false);
    game.setMaxRating(1000);
    game.setMinRating(100);

    //Act
    boolean result = predicate.test(game);

    //Assert
    Assert.assertTrue(result);
  }

  @Test
  public void testMatchesWithin() {
    //Arrange
    game.setEnforceRating(true);
    game.setMaxRating(1200);
    game.setMinRating(1000);

    //Act
    boolean result = predicate.test(game);

    //Assert
    Assert.assertTrue(result);
  }

  @Test
  public void testMatchesOnBorderingMin() {
    //Arrange
    game.setMaxRating(1200);
    game.setMinRating(1100);

    //Act
    boolean result = predicate.test(game);

    //Assert
    Assert.assertTrue(result);
  }

  @Test
  public void testMatchesOnBorderingMax() {
    //Arrange
    game.setMaxRating(1100);
    game.setMinRating(100);

    //Act
    boolean result = predicate.test(game);

    //Assert
    Assert.assertTrue(result);
  }

  @Test
  public void testDoesntMatchOnPlayerRatingAbove() {
    //Arrange
    game.setMaxRating(800);
    game.setMinRating(100);

    //Act
    boolean result = predicate.test(game);

    //Assert
    Assert.assertFalse(result);
  }

  @Test
  public void testDoesntMatchOnPlayerRatingBelow() {
    //Arrange
    game.setMaxRating(1800);
    game.setMinRating(1400);

    //Act
    boolean result = predicate.test(game);

    //Assert
    Assert.assertFalse(result);
  }
}
