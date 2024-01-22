package com.faforever.client.tournament;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.TournamentBeanBuilder;
import com.faforever.client.domain.TournamentBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.TournamentMapper;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.Tournament;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

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
  @Spy
  private final TournamentMapper tournamentMapper = Mappers.getMapper(TournamentMapper.class);
  @InjectMocks
  private TournamentService instance;
  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(tournamentMapper);
  }

  @Test
  public void testAllTournaments() throws Exception {
    TournamentBean tournamentBean = TournamentBeanBuilder.create().defaultValues().get();
    Flux<Tournament> resultFlux = Flux.just(tournamentMapper.map(tournamentBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(eq(Tournament.class), anyString(), anyInt(), any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getAllTournaments()).expectNext(tournamentBean).verifyComplete();
    verify(fafApiAccessor).getMany(eq(Tournament.class), eq("/challonge/v1/tournaments.json"), eq(100), any());
  }
}
