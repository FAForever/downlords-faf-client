package com.faforever.client.tournament;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.TournamentMapper;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.Tournament;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TournamentServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;

  private final TournamentMapper tournamentMapper = Mappers.getMapper(TournamentMapper.class);
  private TournamentService instance;
  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(tournamentMapper);
    instance = new TournamentService(fafApiAccessor, tournamentMapper);
  }

  @Test
  public void testGetClanByTag() throws Exception {
    when(fafApiAccessor.getMany(any(), anyString(), anyInt(), any())).thenReturn(Flux.empty());
    instance.getAllTournaments().join();
    verify(fafApiAccessor).getMany(eq(Tournament.class), eq("/challonge/v1/tournaments.json"), eq(100), any());
  }
}
