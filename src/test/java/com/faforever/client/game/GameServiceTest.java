package com.faforever.client.game;

import com.faforever.client.builders.GameInfoMessageBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.mapstruct.GameMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.GameInfo.TeamIds;
import javafx.beans.property.SimpleObjectProperty;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.scheduler.Schedulers;
import reactor.test.publisher.TestPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.faforever.commons.lobby.GameStatus.CLOSED;
import static com.faforever.commons.lobby.GameStatus.PLAYING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class GameServiceTest extends ServiceTest {

  @InjectMocks
  private GameService instance;

  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private PlayerService playerService;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Spy
  private GameMapper gameMapper = Mappers.getMapper(GameMapper.class);


  private final TestPublisher<com.faforever.commons.lobby.GameInfo> testGamePublisher = TestPublisher.create();

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(gameMapper);

    when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    when(fafServerAccessor.getEvents(com.faforever.commons.lobby.GameInfo.class)).thenReturn(testGamePublisher.flux());
    when(fafServerAccessor.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>());

    instance.afterPropertiesSet();

    testGamePublisher.assertSubscribers(1);
  }

  @Test
  public void testRetryFlux() {
    doThrow(new RuntimeException()).when(gameMapper).update(any(), any());

    testGamePublisher.next(GameInfoMessageBuilder.create(1).defaultValues().get());

    testGamePublisher.assertSubscribers(1);
  }

  @Test
  public void testPlayerLeftOpenGame() {
    PlayerInfo player1 = PlayerInfoBuilder.create().defaultValues().id(1).get();
    PlayerInfo player2 = PlayerInfoBuilder.create().defaultValues().id(2).get();

    when(playerService.getPlayerByIdIfOnline(1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByIdIfOnline(2)).thenReturn(Optional.of(player2));

    List<TeamIds> teamIds = List.of(new TeamIds(1, List.of(1)), new TeamIds(2, List.of(2)));

    testGamePublisher.next(GameInfoMessageBuilder.create(0).defaultValues().teamIds(teamIds).get());
    GameInfo game = instance.getByUid(0).orElseThrow();

    assertThat(player1.getGame(), is(game));
    assertThat(player2.getGame(), is(game));

    game.setTeams(Map.of(2, List.of(2)));

    assertThat(player1.getGame(), is(CoreMatchers.nullValue()));
    assertThat(player2.getGame(), is(game));
  }

  @Test
  public void testOnGames() {
    assertThat(instance.getGames(), empty());

    com.faforever.commons.lobby.GameInfo multiGameInfo = GameInfoMessageBuilder.create(1)
                                                                               .games(List.of(
                                                                                   GameInfoMessageBuilder.create(1)
                                                                                                         .defaultValues()
                                                                                                         .get(),
                       GameInfoMessageBuilder.create(2).defaultValues().get())
        ).get();
    testGamePublisher.next(multiGameInfo);


    assertThat(instance.getGames(), hasSize(2));
  }

  @Test
  public void testOnGameInfoAdd() {
    assertThat(instance.getGames(), empty());

    com.faforever.commons.lobby.GameInfo gameInfo1 = GameInfoMessageBuilder.create(1)
                                                                           .defaultValues()
                                                                           .title("Game 1")
                                                                           .get();
    testGamePublisher.next(gameInfo1);

    com.faforever.commons.lobby.GameInfo gameInfo2 = GameInfoMessageBuilder.create(2)
                                                                           .defaultValues()
                                                                           .title("Game 2")
                                                                           .get();
    testGamePublisher.next(gameInfo2);


    assertThat(instance.getGames(), containsInAnyOrder(
        allOf(
            GameMatchers.hasId(1),
            GameMatchers.hasTitle("Game 1")
        ),
        allOf(
            GameMatchers.hasId(2),
            GameMatchers.hasTitle("Game 2")
        )
    ));
  }

  @Test
  public void testOnGameInfoModify() {
    assertThat(instance.getGames(), empty());

    com.faforever.commons.lobby.GameInfo gameInfo = GameInfoMessageBuilder.create(1)
                                                                          .defaultValues()
                                                                          .title("Game 1")
                                                                          .state(PLAYING)
                                                                          .get();
    testGamePublisher.next(gameInfo);

    GameInfo game = instance.getByUid(1).orElseThrow();

    gameInfo = GameInfoMessageBuilder.create(1).defaultValues().title("Game 1 modified").state(PLAYING).get();
    testGamePublisher.next(gameInfo);

    assertEquals(gameInfo.getTitle(), game.getTitle());
  }

  @Test
  public void testOnGameInfoRemove() {
    assertThat(instance.getGames(), empty());

    com.faforever.commons.lobby.GameInfo gameInfo = GameInfoMessageBuilder.create(1)
                                                                          .defaultValues()
                                                                          .title("Game 1")
                                                                          .get();
    testGamePublisher.next(gameInfo);
    assertThat(instance.getGames(), hasSize(1));

    gameInfo = GameInfoMessageBuilder.create(1).title("Game 1").defaultValues().state(CLOSED).get();
    testGamePublisher.next(gameInfo);

    assertThat(instance.getGames(), empty());
  }
}
