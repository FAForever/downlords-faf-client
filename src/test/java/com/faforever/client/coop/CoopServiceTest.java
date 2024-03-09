package com.faforever.client.coop;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.api.CoopMission;
import com.faforever.client.domain.api.CoopResult;
import com.faforever.client.mapstruct.CoopMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.elide.ElideEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class CoopServiceTest extends ServiceTest {

  @InjectMocks
  private CoopService instance;

  @Mock
  private FafApiAccessor fafApiAccessor;

  @Spy
  private final CoopMapper coopMapper = Mappers.getMapper(CoopMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(coopMapper);

    instance = new CoopService(fafApiAccessor, coopMapper);
  }

  @Test
  public void testGetCoopMaps() throws Exception {
    CoopMission coopMission = Instancio.create(CoopMission.class);

    Flux<ElideEntity> resultFlux = Flux.just(coopMapper.map(coopMission));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getMissions()).expectNext(coopMission).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1000)));
  }

  @Test
  public void testGetLeaderboard() throws Exception {
    CoopResult coopResult = Instancio.of(CoopResult.class)
                                     .set(field(CoopResult::ranking), 0)
                                     .ignore(field(CoopResult::replay))
                                     .create();

    com.faforever.commons.api.dto.CoopResult result = coopMapper.map(coopResult);
    Flux<ElideEntity> resultFlux = Flux.just(result, result);
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    CoopMission mission = Instancio.create(CoopMission.class);
    StepVerifier.create(instance.getLeaderboard(mission, 2)).expectNext(coopResult).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1000)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("mission").eq(mission.id())
        .and().intNum("playerCount").eq(2))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("duration", true)));
  }
}
